package com.pol.codebot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceiveFile extends Command {
    public ReceiveFile(MessageReceivedEvent event, String[] command) {
        this.event = event;
        this.command = command;
    }

    @Override
    public void setCommand() {
        AtomicInteger secondsLeft = new AtomicInteger(90);
        event.getChannel().sendMessage(
                timer(secondsLeft.get(), "Upload a source code file within %d seconds for Code Bot to run").build())
                .queue((Message msg) -> {
                    Timer fileTimer = new Timer();
                    ListenerAdapter listener = fileListener(fileTimer);
                    Main.jda.addEventListener(listener);
                    fileTimerTask(msg, listener, secondsLeft, fileTimer,
                            "Upload a source code file within %d seconds for Code Bot to run");
                });
    }

    private ListenerAdapter fileListener(Timer fileTimer) {
        return new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                List<Message.Attachment> attachments = event.getMessage().getAttachments();
                if (!(ReceiveFile.super.event.getAuthor().getId().equals(event.getAuthor().getId())
                        && ReceiveFile.super.event.getChannel().getId().equals(event.getChannel().getId()))
                        || attachments.size() != 1) {
                    return;
                }
                try {
                    String code = new String(attachments.get(0).retrieveInputStream().get().readAllBytes(),
                            StandardCharsets.UTF_8);
                    detectFileExtensionAndRun(attachments.get(0).getFileExtension(), code);
                    fileTimer.cancel();
                    Main.jda.removeEventListener(this);
                } catch (IOException | InterruptedException | ExecutionException e) {
                    ReceiveFile.super.unexpectedError(e.toString());
                }
            }
        };
    }

    private ListenerAdapter languageListener(String code) {
        return new ListenerAdapter() {
            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                if (!(ReceiveFile.super.event.getAuthor().getId().equals(event.getAuthor().getId())
                        && ReceiveFile.super.event.getChannel().getId().equals(event.getChannel().getId()))) {
                    return;
                }
                String language = event.getMessage().getContentStripped();
                runCodeInLanguage(language, code, event);
                Main.jda.removeEventListener(this);
            }
        };
    }

    private boolean runCodeInLanguage(String language, String code, MessageReceivedEvent event) {
        Languages languages = Main.stringToLanguage(language);
        if (languages == null) {
            return false;
        }
        new Programming(event, code, languages).start();
        return true;
    }

    private void fileTimerTask(Message msg, ListenerAdapter listener, AtomicInteger secondsLeft, Timer fileTimer,
            String message) {
        TimerTask fileTimerTask = new TimerTask() {
            @Override
            public void run() {
                int time = secondsLeft.addAndGet(-5);
                if (time < 0) {
                    fileTimer.cancel();
                    Main.jda.removeEventListener(listener);
                    return;
                }
                msg.editMessage(timer(time, message).build()).queue();
            }
        };
        fileTimer.schedule(fileTimerTask, Calendar.getInstance().getTime(), 5000);
    }

    private void detectFileExtensionAndRun(String extension, String code) {
        if (!runCodeInLanguage(extension, code, event)) {
            AtomicInteger secondsLeft = new AtomicInteger(30);
            event.getChannel()
                    .sendMessage(timer(secondsLeft.get(),
                            "State the language that the file was written in within %d seconds for Code Bot to run")
                                    .build())
                    .queue((Message msg) -> {
                        Timer fileTimer = new Timer();
                        ListenerAdapter languageListener = languageListener(code);
                        Main.jda.addEventListener(languageListener);
                        fileTimerTask(msg, languageListener, secondsLeft, fileTimer,
                                "State the language that the file was written in within %d seconds for Code Bot to run");
                    });
        }
    }

    private EmbedBuilder timer(int secondsLeft, String message) {
        return new EmbedBuilder().setTitle("File Upload").setDescription(String.format(message, secondsLeft)).setFooter(
                String.format("Created by %s", event.getAuthor().getName()), event.getAuthor().getAvatarUrl());
    }

}
