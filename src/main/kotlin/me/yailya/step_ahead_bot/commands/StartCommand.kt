package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row

suspend fun BehaviourContext.handleStartCommand(message: TextMessage) {
    reply(
        to = message,
        text = "Приветствуем вас!",
        replyMarkup = inlineKeyboard {
            row {
                dataButton("Посмотреть доступные ВУЗы", "universities")
            }

            row {
                dataButton("Мои запросы на изменение информации", "update_requests")
            }
        }
    )
}