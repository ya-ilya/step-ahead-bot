package me.yailya.step_ahead_bot.commands

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import me.yailya.step_ahead_bot.university.Universities

@CommandHandler(["/start"])
suspend fun handleStartCommand(user: User, bot: TelegramBot) {
    message { "Приветствуем вас! Выберете один из ВУЗов:" }.inlineKeyboardMarkup {
        for (university in Universities) {
            if (university.key % 4 == 0) {
                newLine()
            }

            "(${university.key}) ${university.value.shortName}" callback "university_${university.key}"
        }
    }.send(user, bot)
}