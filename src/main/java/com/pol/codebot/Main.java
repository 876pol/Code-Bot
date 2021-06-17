package com.pol.codebot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.entities.Message;
import org.springframework.boot.SpringApplication;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class Main extends ListenerAdapter {
    public static JDA jda;
    public static String os;

    public static void main(String[] args) throws LoginException {
        startSpringBoot();
        os = detectOSAndLog();
        if (Main.os.equals("Linux") && !setWritableRootAndReturnSuccess()) {
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "'chmod 500 $(ls)' failed");
        }
        startSelfPingTimer();
        startJda();
    }

    private static void startSpringBoot() {
        if (System.getenv().containsKey("PORT")) {
            SpringApplication.run(Main.class, "--server.port=" + System.getenv("PORT"));
        } else {
            SpringApplication.run(Main.class, "--server.port=80");
        }
    }

    private static String detectOSAndLog() {
        String opSys = System.getProperty("os.name").toLowerCase().contains("win") ? "Windows" : "Linux";
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "OS Detected: " + opSys);
        return opSys;
    }

    private static boolean setWritableRootAndReturnSuccess() {
        try {
            Runtime.getRuntime().exec(
                    "chmod 500 'Code Bot.jar' bin boot etc home lib lib64 media mnt opt root run sbin srv usr var");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static void startJda() throws LoginException {
        jda = JDABuilder.createDefault(Keys.getDiscord()).addEventListeners(new Main())
                .setActivity(Activity.playing("code help")).setStatus(OnlineStatus.ONLINE).build();
    }

    private static void startSelfPingTimer() {
        new Timer().schedule(new SelfPing(), Calendar.getInstance().getTime(), 1200000);
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        String[] message = event.getMessage().getContentStripped().toLowerCase().split(" ");
        if (!message[0].equals("code") || event.getAuthor().isBot()) {
            return;
        }
        switch (message[1]) {
            case "run": {
                try {
                    new Programming(event, parseCodeAndReturn(event), stringToLanguage(message[2])).start();
                } catch (NullPointerException ignored) {}
                break;
            }
            case "judge":
                new Judge(event, parseCodeAndReturn(event), stringToLanguage(message[2]), 0L).start();
                break;
            case "ping":
                new Ping(event).start();
                break;
            case "help":
                new Help(event).start();
                break;
            default:
                EmbedBuilder eb = new EmbedBuilder().setTitle("Invalid Syntax")
                        .setFooter("Use *code help* for all commands");
                event.getChannel().sendMessage(eb.build()).queue();

        }
        System.gc();
    }

    public static Languages stringToLanguage(String language) {
        switch (language) {
            case "c":
                return Languages.C;
            case "c++":
            case "cpp":
                return Languages.CPP;
            case "python2":
                return Languages.PYTHON2;
            case "python3":
            case "python":
            case "py":
                return Languages.PYTHON3;
            case "java":
                return Languages.JAVA;
            case "bash":
            case "sh":
                return Languages.SHELL;
            case "javascript":
            case "js":
            case "node":
                return Languages.JS;
            case "html":
            case "htm":
                return Languages.HTML;
        }
        return null;
    }

    public static String convertStringArrayToString(String[] stringArray, int startIndex) {
        StringBuilder retVal = new StringBuilder();
        IntStream.range(startIndex, stringArray.length).forEach(i -> retVal.append(stringArray[i]).append(" "));
        return retVal.substring(0, retVal.length() - 1);
    }

    private static String parseCodeAndReturn(MessageReceivedEvent event) {
        try {
            String code = event.getMessage().getContentStripped();
            String parsedCode = convertStringArrayToString(code.split(" "), 3);
            if (parsedCode.startsWith("c++")) {
                parsedCode = parsedCode.substring(3);
            }
            return parsedCode;
        } catch (StringIndexOutOfBoundsException e) {
            List<Message.Attachment> attachments = event.getMessage().getAttachments();
            if (attachments.size() != 1) {
                return null;
            }
            try {
                return new String(attachments.get(0).retrieveInputStream().get().readAllBytes(),
                        StandardCharsets.UTF_8);
            } catch (IOException | InterruptedException | ExecutionException e1) {
                return null;
            }
        }
    }
}
