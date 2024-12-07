@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.question.handlers

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
import me.yailya.step_ahead_bot.question.QuestionEntity

suspend fun BehaviourContext.handleQuestionDeleteCallback(
    query: DataCallbackQuery,
    questionId: Int
) {
    val otherBotUser = query.botUser()

    databaseQuery {
        val question = QuestionEntity.findById(questionId)

        if (question == null) {
            answerCallbackQuery(
                query,
                "❌ Данного вопроса не существует"
            )

            return@databaseQuery
        }

        if (question.botUser.id != otherBotUser.first.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете удалить не ваш вопрос"
            )

            return@databaseQuery
        }

        databaseQuery {
            question.delete()
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

            val (previous, other, next) = questionForKeyboard(query, otherId)

            edit(
                query = query,
                entities = buildEntities {
                    +bold("${other.university.shortName} -> Вопрос #${other.id}") +
                            "\n" + other.text
                },
                replyMarkup = inlineKeyboard {
                    row {
                        dataButton(
                            "\uD83D\uDE4B\uD83C\uDFFB\u200D♂\uFE0F Посмотреть ответы",
                            "Question_QuestionAnswers_${other.id}"
                        )
                    }
                    row {
                        dataButton("\uD83D\uDDD1\uFE0F Удалить", "Question_delete_${other.id}")
                    }
                    row {
                        if (previous != null) {
                            dataButton("⬅\uFE0F Предыдущий", "Question_${other.id}")
                        }
                        if (next != null) {
                            dataButton("Следующий ➡\uFE0F", "Question_${other.id}")
                        }
                    }
                }
            )
        } catch (ex: Exception) {
            deleteMessage(query.message!!)
        }

        answerCallbackQuery(
            query,
            "✅ Ваш вопрос #${questionId} был удален"
        )
    }
}