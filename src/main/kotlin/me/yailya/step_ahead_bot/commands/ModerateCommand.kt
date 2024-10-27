package me.yailya.step_ahead_bot.commands

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import me.yailya.step_ahead_bot.moderator.ModeratorEntity

suspend fun handleModerateCommand(user: User, bot: TelegramBot) {
    val moderator = ModeratorEntity.getModeratorByUserId(user.id) ?: return

    message { "Здраствуйте, модератор #${moderator.id.value}. Выберете нужную вам опцию:" }.inlineKeyboardMarkup {
        "Рассмотреть открытые запросы на изменение" callback "moderate_update_requests"
    }.send(user, bot)
}