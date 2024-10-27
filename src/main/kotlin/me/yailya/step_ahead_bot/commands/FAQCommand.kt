package me.yailya.step_ahead_bot.commands

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User

suspend fun handleFAQCommand(user: User, bot: TelegramBot) {
    message {
        "Вопрос/ответ:" -
                "\n1. Как увидеть информацию о каком-либо ВУЗе: используйте комманду /start, а затем используйте функциональную клавишу с кратким названием нужного ВУЗа."
    }.send(user, bot)
}