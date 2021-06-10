package com.pol.codebot;

import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Programming extends Command {
    private final String code;
    private final Languages language;
    private final StringBuffer output;
    private final AtomicReference<String> outputString;
    private final AtomicInteger outputLength;
    private final AtomicInteger timeoutLeft;

    public Programming(MessageReceivedEvent event, String code, Languages language) {
        this.event = event;
        this.code = code;
        this.language = language;
        output = new StringBuffer("\u200B");
        outputString = new AtomicReference<>(output.toString());
        outputLength = new AtomicInteger(output.length());
        timeoutLeft = new AtomicInteger(120);
    }

    @Override
    public void setCommand() throws Exception {
        if (language == Languages.HTML) {
            new HTML(event, code).start();
            return;
        }
        File file = createFile();
        makeRestAction().queue((Message msg) -> {
            try {
                Process process = Objects.requireNonNull(codeProcessBuilder(file, msg)).start();
                BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                Timer updateRemainingTimer = updateRemainingTimer(process);
                ListenerAdapter inputListener = inputListener(msg, in, process);
                Main.jda.addEventListener(inputListener);
                Timer updateOutputTimer = updateOutputTimer(msg);
                updateOutput(out);
                Main.jda.removeEventListener(inputListener);
                process.waitFor();
                deleteFileAndLog(file);
                output.append("\nprogram terminated with exit code ").append(process.exitValue());
                Logger.getLogger(Programming.class.getName()).log(Level.INFO,
                        String.format("Language: %s | File: %s", language.name(), file.getParentFile()));
                updateOutputTimer.cancel();
                updateRemainingTimer.cancel();
                in.close();
                out.close();
                outputString.set(output.toString());
                outputLength.set(output.length());
                msg.editMessage(makeEmbed(outputLength.get(), outputString.get()).build()).queue();
            } catch (IOException | IllegalStateException | InterruptedException e) {
                unexpectedError(e.toString());
            }
        });
    }

    private File createFile() throws IOException {
        Path tempDir = Files.createTempDirectory(null);
        File codeFile = new File(tempDir + "/Main" + language.getFileExtension());
        codeFile.deleteOnExit();
        FileWriter fileWriter = new FileWriter(codeFile);
        fileWriter.write(code);
        fileWriter.close();
        return codeFile;
    }

    private ProcessBuilder codeProcessBuilder(File file, Message msg) throws IOException, InterruptedException {
        Pair<ProcessBuilder, String> processBuilderOutputPair = language.getCompileAndReturnCommandAndSendErrorMessage().invoke(file);
        ProcessBuilder runCommandBuilder = processBuilderOutputPair.getFirst();
        output.append(processBuilderOutputPair.getSecond());
        outputString.set(output.toString());
        outputLength.set(output.length());
        msg.editMessage(makeEmbed(outputLength.get(), outputString.get()).build()).queue();
        if (runCommandBuilder == null) {
            Logger.getLogger(Programming.class.getName())
                    .log(Level.WARNING, "Compilation Error | File: " + file.getParentFile());
            return null;
        }
        runCommandBuilder.directory(file.getParentFile());
        runCommandBuilder.redirectErrorStream(true);
        return runCommandBuilder;
    }

    private MessageAction makeRestAction() throws IOException {
        MessageAction restAction = event.getChannel()
                .sendMessage(makeEmbed(outputLength.get(), outputString.get()).build());
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream attachment = classloader.getResourceAsStream(language.getImagePath());
        if (attachment != null && attachment.available() > 0) {
            restAction = restAction.addFile(attachment, "image.png");
        }
        return restAction;
    }

    private Timer updateRemainingTimer(Process process) {
        TimerTask updateRemainingTask = new TimerTask() {
            @Override
            public void run() {
                timeoutLeft.addAndGet(-10);
                if (timeoutLeft.get() < 0) {
                    process.destroyForcibly();
                }
            }
        };
        Timer updateRemainingTimer = new Timer();
        updateRemainingTimer.schedule(updateRemainingTask, Calendar.getInstance().getTime(), 10000);
        return updateRemainingTimer;
    }

    private EmbedBuilder makeEmbed(int outputLength, String outputString) {
        User author = event.getAuthor();
        return new EmbedBuilder().setTitle(String.format("%s's %s Program", author.getName(), language.getLangName()))
                .setThumbnail("attachment://image.png")
                .setDescription("```"
                        + (outputLength > 1000 ? outputString.substring(outputLength - 1000) : outputString) + "```")
                .setFooter(String.format("Created by %s", author.getName()), author.getAvatarUrl());
    }

    private ListenerAdapter inputListener(Message msg, BufferedWriter in, Process process) {
        msg.addReaction("U+274C").queue();
        return new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                try {
                    if (event.getChannel().getId().equals(Programming.super.event.getChannel().getId())
                            && event.getAuthor().getId().equals(Programming.super.event.getAuthor().getId())) {
                        String userInput = event.getMessage().getContentRaw() + "\n";
                        output.append(userInput);
                        in.write(userInput);
                        in.flush();
                        outputString.set(output.toString());
                        outputLength.set(output.length());
                        msg.editMessage(makeEmbed(outputLength.get(), outputString.get()).build()).queue();
                    }
                } catch (IOException | IllegalStateException e) {
                    Programming.super.unexpectedError(e.toString());
                }
            }

            @Override
            public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
                if (Programming.this.event.getAuthor().getId().equals(Objects.requireNonNull(event.getUser()).getId())
                        && event.getMessageId().equals(msg.getId())) {
                    String emote = event.getReactionEmote().toString().toLowerCase();
                    if (emote.contains("u+274c")) {
                        process.destroyForcibly();
                    }
                }
            }
        };
    }

    private Timer updateOutputTimer(Message msg) {
        TimerTask updateOutput = new TimerTask() {
            String pastStr = output.toString();

            @Override
            public void run() {
                try {
                    outputString.set(output.toString());
                    outputLength.set(output.length());
                    if (!pastStr.equals(outputString.get())) {
                        timeoutLeft.set(120);
                        msg.editMessage(makeEmbed(outputLength.get(), outputString.get()).build()).queue();
                    }
                    pastStr = outputString.get();
                } catch (IllegalStateException e) {
                    unexpectedError(e.toString());
                }
            }
        };
        Timer updateOutputTimer = new Timer();
        updateOutputTimer.schedule(updateOutput, Calendar.getInstance().getTime(), 2000);
        return updateOutputTimer;
    }

    private void deleteFileAndLog(File file) {
        if (!file.getParentFile().setWritable(true)) {
            Logger.getLogger(Programming.class.getName()).log(Level.WARNING,
                    String.format("File Set Writable Failed: %s", file.getParent()));
        }
        if (!FileSystemUtils.deleteRecursively(file.getParentFile())) {
            Logger.getLogger(Programming.class.getName()).log(Level.WARNING,
                    String.format("File Delete Failed: %s", file.getParent()));
        }
    }

    private void updateOutput(BufferedReader out) throws IOException {
        int stdout;
        while ((stdout = out.read()) != -1) {
            output.append((char) stdout);
        }
    }

}

class HTML extends Command {
    static HashMap<String, String> fileMap = new HashMap<>();
    private final String code;
    private final InputStream attachment;

    public HTML(MessageReceivedEvent event, String code) {
        this.event = event;
        this.code = code;
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        this.attachment = classloader.getResourceAsStream("logos/html.png");
    }

    private String randomString() {
        int leftLimit = 48;
        int rightLimit = 122;
        int targetStringLength = 10;
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Override
    public void setCommand() throws Exception {
        String key = randomString();
        while (fileMap.containsKey(key)) {
            key = randomString();
        }
        String url = String.format("%shtml/%s", SelfPing.url, key);
        fileMap.put(key, code);
        MessageAction restAction =
                event.getChannel().sendMessage(makeEmbed(url).build());
        if (attachment.available() > 0) {
            restAction = restAction.addFile(attachment, "image.png");
        }
        restAction.queue();
        String finalKey = key;
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (!fileMap.remove(finalKey, code)) {
                            Logger.getLogger(Programming.class.getName()).log(Level.WARNING, "File Delete Failed");
                        }
                    }
                }, 600000
        );
    }

    private EmbedBuilder makeEmbed(String url) {
        User author = event.getAuthor();
        return new EmbedBuilder()
                .setTitle(String.format("%s's Webpage", author.getName()))
                .setThumbnail("attachment://image.png")
                .setDescription(url)
                .setFooter(String.format("Created by %s\nThis link will work for the next 10 minutes",
                        author.getName()), author.getAvatarUrl());
    }
}
