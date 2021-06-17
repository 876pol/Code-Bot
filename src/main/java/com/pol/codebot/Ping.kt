package com.pol.codebot

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent


class Ping(event: MessageReceivedEvent) : Command() {
    init {
        this.event = event
    }

    override fun setCommand() {
        val channel = event.channel
        val time = System.currentTimeMillis()
        channel.sendMessage("Ping:")
            .queue { response: Message ->
                response.editMessageFormat("Ping: %d ms", System.currentTimeMillis() - time).queue()
            }
    }

    override fun invalidCommand(i: Int) {}
}