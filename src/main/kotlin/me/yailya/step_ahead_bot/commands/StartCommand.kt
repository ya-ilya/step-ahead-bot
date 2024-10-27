package me.yailya.step_ahead_bot.commands

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User

suspend fun handleStartCommand(user: User, bot: TelegramBot) {
    message { "Приветствуем вас!" }.inlineKeyboardMarkup {
        "Посмотреть доступные ВУЗы" callback "universities"
        newLine()
        "Мои запросы на изменение информации" callback "update_requests"
    }.send(user, bot)
}