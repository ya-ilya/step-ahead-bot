package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.moderator.ModeratorEntity

suspend fun BehaviourContext.handleModerateCommand(message: TextMessage) {
    val moderator = ModeratorEntity.getModeratorByUserId(message.chat.id.chatId.long) ?: return

    reply(
        to = message,
        text = "Здраствуйте, модератор #${moderator.id.value}. Выберете нужную вам опцию:",
        replyMarkup = inlineKeyboard {
            row {
                dataButton("Рассмотреть открытые запросы на изменение", "moderate_update_requests")
            }
        }
    )
}