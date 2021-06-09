package com.pol.codebot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command extends Thread {
    public MessageReceivedEvent event;
    public String[] command;

    @Override
    public void run() {
        try {
            event.getChannel().sendTyping().queue();
        } catch (NullPointerException e) {
            e.printStackTrace();
            unexpectedError(e.toString());
        }
        try {
            setCommand();
        } catch (Exception e) {
            e.printStackTrace();
            unexpectedError(e.toString());
        }
    }

    public void unexpectedError(String error) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("An unexpected error has occurred")
                .setDescription(error)
                .setFooter("contact @876pol#1477 for this issue");
        event.getChannel().sendMessage(eb.build()).queue();
    }

    public abstract void setCommand() throws Exception;

}