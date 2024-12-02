@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.question.answer.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.edit
import me.yailya.step_ahead_bot.question.answer.AnswerEntity

suspend fun BehaviourContext.handleAnswerDeleteCallback(
    query: DataCallbackQuery,
    answerId: Int
) {
    val (otherBotUser) = query.botUser()

    databaseQuery {
        val answer = AnswerEntity.findById(answerId)

        if (answer == null) {
            answerCallbackQuery(
                query,
                "❌ Данного ответа на вопрос не существует"
            )

            return@databaseQuery
        }

        if (answer.botUser.id != otherBotUser.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете удалить не ваш ответ на вопрос"
            )

            return@databaseQuery
        }

        databaseQuery {
            answer.delete()
        }

        try {
            val row = query
                .message!!
                .reply_markup!!
                .keyboard[2]

            val data = row
                .filterIsInstance<CallbackDataInlineKeyboardButton>()
                .first { it.text.contains("Следующий") || it.text.contains("Предыдущий") }
                .callbackData

            val otherId = data
                .split("_")
                .last()
                .toInt()

            val (previous, other, next) = answerForKeyboard(query, otherId)

            edit(
                query = query,
                entities = buildEntities {
                    +bold("Ответ на вопрос #${other.id}${if (other.isAccepted) ". Помечен, как одобренный" else ""}") +
                            "\n" + other.text
                },
                replyMarkup = inlineKeyboard {
                    row {
                        dataButton("❔ Посмотреть вопрос", "Answer_question_${other.id}")
                    }
                    row {
                        dataButton("\uD83D\uDDD1\uFE0F Удалить", "Answer_delete_${other.id}")
                    }
                    row {
                        if (previous != null) {
                            dataButton("⬅\uFE0F Предыдущий", "Answer_${previous.id}")
                        }
                        if (next != null) {
                            dataButton("Следующий ➡\uFE0F", "Answer_${next.id}")
                        }
                    }
                }
            )
        } catch (ex: Exception) {
            deleteMessage(query.message!!)
        }

        answerCallbackQuery(
            query,
            "✅ Ваш ответ на вопрос #${answerId} был удален"
        )
    }

    answerCallbackQuery(query)
}