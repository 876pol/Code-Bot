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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Programming extends Command {
    private final String code;
    private final Languages language;
    private final StringBuffer output;
    private final AtomicReference<Timer> timeoutLeftTimer;
    private File codeFile;

    public Programming(MessageReceivedEvent event, String code, Languages language) {
        this.event = event;
        this.code = code;
        this.language = language;
        this.output = new StringBuffer("\u200B");
        this.timeoutLeftTimer = new AtomicReference<>();
    }

    @Override
    public void setCommand() throws Exception {
        if (language == Languages.HTML) {
            new HTML(event, code).start();
            return;
        }
        generateRestAction().queue((Message msg) -> {
            try {
                codeFile = generateCodeDirectory();
                Process process = Objects.requireNonNull(generateCodeProcessBuilder(msg)).start();
                BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                Timer updateOutputTimer = generateUpdateOutputTimer(msg);
                timeoutLeftTimer.set(generateUpdateRemainingTimer(process));
                ListenerAdapter inputListener = inputListener(msg, in, process);
                Main.jda.addEventListener(inputListener);
                Logger.getLogger(Programming.class.getName()).log(Level.INFO,
                        String.format("Language: %s | File: %s", language.name(), codeFile.getParentFile()));
                updateOutput(out);
                process.waitFor();
                deleteFileAndLog();
                output.append("\nprogram terminated with exit code ").append(process.exitValue());
                Main.jda.removeEventListener(inputListener);
                updateOutputTimer.cancel();
                timeoutLeftTimer.get().cancel();
                in.close();
                out.close();
                msg.editMessage(generateEmbed().build()).queue();
            } catch (IOException | IllegalStateException | InterruptedException e) {
                unexpectedError(e.toString());
            }
        });
    }

    private File generateCodeDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory(null);
        File codeFile = new File(tempDir + "/Main" + language.getFileExtension());
        codeFile.deleteOnExit();
        FileWriter fileWriter = new FileWriter(codeFile);
        fileWriter.write(code);
        fileWriter.close();
        return codeFile;
    }

    private ProcessBuilder generateCodeProcessBuilder(Message msg) throws IOException, InterruptedException {
        Pair<ProcessBuilder, String> processBuilderOutputPair =
                language.getCompileAndReturnCommandAndSendErrorMessage().invoke(codeFile);
        ProcessBuilder runCommandBuilder = processBuilderOutputPair.getFirst();
        output.append(processBuilderOutputPair.getSecond());
        msg.editMessage(generateEmbed().build()).queue();
        if (runCommandBuilder == null) {
            Logger.getLogger(Programming.class.getName())
                    .log(Level.WARNING, "Compilation Error | File: " + codeFile.getParentFile());
            return null;
        }
        runCommandBuilder.directory(codeFile.getParentFile());
        runCommandBuilder.redirectErrorStream(true);
        return runCommandBuilder;
    }

    private MessageAction generateRestAction() throws IOException {
        MessageAction restAction = event.getChannel()
                .sendMessage(generateEmbed().build());
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream attachment = classloader.getResourceAsStream(language.getImagePath());
        if (attachment != null && attachment.available() > 0) {
            restAction = restAction.addFile(attachment, "image.png");
        }
        return restAction;
    }

    private Timer generateUpdateRemainingTimer(Process process) {
        TimerTask updateRemainingTask = new TimerTask() {
            @Override
            public void run() {
                process.destroyForcibly();
            }
        };
        Timer updateRemainingTimer = new Timer();
        updateRemainingTimer.schedule(updateRemainingTask, 120000);
        return updateRemainingTimer;
    }

    private Timer generateUpdateOutputTimer(Message msg) {
        TimerTask updateOutput = new TimerTask() {
            String pastStr = output.toString();

            @Override
            public void run() {
                if (!pastStr.equals(output.toString())) {
                    msg.editMessage(generateEmbed().build()).queue();
                    pastStr = output.toString();
                }
            }
        };
        Timer updateOutputTimer = new Timer();
        updateOutputTimer.schedule(updateOutput, Calendar.getInstance().getTime(), 3000);
        return updateOutputTimer;
    }

    private EmbedBuilder generateEmbed() {
        String outputString = output.toString();
        int outputLength = output.length();
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
                        timeoutLeftTimer.get().cancel();
                        timeoutLeftTimer.set(generateUpdateRemainingTimer(process));
                        msg.editMessage(generateEmbed().build()).queue();
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

    private void deleteFileAndLog() {
        if (!codeFile.getParentFile().setWritable(true)) {
            Logger.getLogger(Programming.class.getName()).log(Level.WARNING,
                    String.format("File Set Writable Failed: %s", codeFile.getParent()));
        }
        if (!FileSystemUtils.deleteRecursively(codeFile.getParentFile())) {
            Logger.getLogger(Programming.class.getName()).log(Level.WARNING,
                    String.format("File Delete Failed: %s", codeFile.getParent()));
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
    public static HashMap<String, String> fileMap = new HashMap<>();
    private final String code;
    private final String key;
    private final InputStream attachment;

    public HTML(MessageReceivedEvent event, String code) {
        String key1;
        this.event = event;
        this.code = code;
        key1 = generateKey();
        while (fileMap.containsKey(key1)) {
            key1 = generateKey();
        }
        this.key = key1;
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        this.attachment = classloader.getResourceAsStream("images/logos/html.png");
    }

    private String generateKey() {
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
        String url = String.format("%shtml/%s", SelfPing.url, key);
        fileMap.put(key, code);
        MessageAction restAction =
                event.getChannel().sendMessage(generateEmbed(url).build());
        if (attachment.available() > 0) {
            restAction = restAction.addFile(attachment, "image.png");
        }
        restAction.queue();
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (!fileMap.remove(key, code)) {
                            Logger.getLogger(Programming.class.getName()).log(Level.WARNING, "File Delete Failed");
                        }
                    }
                }, 600000
        );
    }

    private EmbedBuilder generateEmbed(String url) {
        User author = event.getAuthor();
        return new EmbedBuilder()
                .setTitle(String.format("%s's Webpage", author.getName()))
                .setThumbnail("attachment://image.png")
                .setDescription(url)
                .setFooter(String.format("Created by %s\nThis link will work for the next 10 minutes",
                        author.getName()), author.getAvatarUrl());
    }
}