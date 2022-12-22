@file:JvmName("MainKt")
package net.azisaba.azisabareportbot

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ApplicationCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.kordLogger
import dev.kord.core.on
import net.azisaba.azisabareportbot.commands.CloseCommand
import net.azisaba.azisabareportbot.commands.InfoCommand
import net.azisaba.azisabareportbot.util.Util

suspend fun main() {
    val client = Kord(Util.getEnvOrThrow("BOT_TOKEN"))

    client.createGlobalApplicationCommands {
        InfoCommand.register(this)
        CloseCommand.register(this)
    }

    client.on<ApplicationCommandInteractionCreateEvent> {
        if (interaction.user.isBot) return@on
        try {
            if (interaction.invokedCommandName == "info") InfoCommand.handle(interaction)
            if (interaction.invokedCommandName == "close") CloseCommand.handle(interaction)
        } catch (e: Throwable) {
            e.printStackTrace()
            interaction.respondEphemeral { content = "エラーが発生しました。" }
        }
    }

    client.on<ButtonInteractionCreateEvent> {
        if (interaction.user.isBot) return@on
        try {
            if (interaction.componentId.startsWith("info:")) InfoCommand.handleButtonInteraction(interaction)
        } catch (e: Throwable) {
            e.printStackTrace()
            interaction.respondEphemeral { content = "エラーが発生しました。" }
        }
    }

    client.on<ReadyEvent> {
        println("Logged in as ${kord.getSelf().tag}!")
    }

    client.login()
}
