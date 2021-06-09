package com.pol.codebot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Help extends Command {

    public Help(MessageReceivedEvent event) {
        this.event = event;
    }

    @Override
    public void setCommand() {
        event.getChannel().sendMessage(helpEmbed().build()).queue();
    }

    private EmbedBuilder helpEmbed() {
        User author = event.getAuthor();
        return new EmbedBuilder()
        .setTitle("Code Bot Help")
        .setDescription("Here's the link to our help page:\n" + SelfPing.url + "help")
        .setFooter("Created by " + author.getName(), author.getAvatarUrl());
    }

}
