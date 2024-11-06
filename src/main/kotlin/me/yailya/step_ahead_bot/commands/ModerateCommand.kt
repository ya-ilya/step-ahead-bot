package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery

suspend fun BehaviourContext.handleModerateCommand(message: TextMessage) {
    val (_, botUser) = message.botUser()

    if (!databaseQuery { botUser.isModerator }) {
        return
    }

    reply(
        to = message,
        text = "Здраствуйте, модератор #${botUser.id}. Выберете нужную вам опцию:",
        replyMarkup = inlineKeyboard {
            row {
                dataButton("\uD83D\uDD0D Рассмотреть открытые запросы на изменение", "moderate_update_requests")
            }
        }
    )
}