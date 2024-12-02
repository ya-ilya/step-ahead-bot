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
                dataButton("❔ Мои вопросы", "Questions")
            }

            row {
                dataButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂\uFE0F Мои ответы на вопросы", "Answers")
            }

            row {
                dataButton("⭐ Мои отзывы", "UniversityReviews")
            }

            row {
                dataButton("⭐ Мои отзывы о преподавателях", "TeacherReviews")
            }

            row {
                dataButton("\uD83C\uDD95 Мои запросы на изменение информации о вузах", "UniversityUpdateRequests")
            }

            row {
                dataButton("\uD83C\uDD95 Мои запросы на добавление новых преподавателей", "AddTeacherRequests")
            }
        }
    )
}