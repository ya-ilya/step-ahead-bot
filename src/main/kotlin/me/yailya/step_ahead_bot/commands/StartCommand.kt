package me.yailya.step_ahead_bot.commands

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User

@CommandHandler(["/start"])
suspend fun handleStartCommand(user: User, bot: TelegramBot) {
    message { "Приветсвуем вас!" }.send(user, bot)
}