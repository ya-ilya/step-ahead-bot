@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.teacher.review.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.edit
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity

suspend fun BehaviourContext.handleTeacherReviewDeleteCallback(
    query: DataCallbackQuery,
    teacherReviewId: Int
) {
    val (otherBotUser) = query.botUser()

    databaseQuery {
        val teacherReview = TeacherReviewEntity.findById(teacherReviewId)

        if (teacherReview == null) {
            answerCallbackQuery(
                query,
                "❌ Данного отзыва о преподавателе не существует"
            )

            return@databaseQuery
        }

        if (teacherReview.botUser.id != otherBotUser.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете удалить не ваш отзыв о преподавателе"
            )

            return@databaseQuery
        }

        databaseQuery {
            teacherReview.delete()
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

            val (previous, other, next) = teacherReviewForKeyboard(query, otherId)

            edit(
                query = query,
                entities = buildEntities {
                    +bold("Отзыв о преподавателе #${other.id}. ${other.rating}/5") +
                            "\n" + blockquote(other.comment)
                },
                replyMarkup = inlineKeyboard {
                    row {
                        dataButton(
                            "\uD83D\uDC69\u200D\uD83C\uDFEB Посмотреть информацию о преподавателе",
                            "teacher_review_teacher_${other.id}"
                        )
                    }

                    row {
                        dataButton("\uD83D\uDDD1\uFE0F Удалить", "teacher_review_delete_${other.id}")
                    }

                    row {
                        if (previous != null) {
                            dataButton("⬅\uFE0F Предыдущий", "teacher_review_${previous.id}")
                        }
                        if (next != null) {
                            dataButton("Следующий ➡\uFE0F", "teacher_review_${next.id}")
                        }
                    }
                }
            )
        } catch (ex: Exception) {
            deleteMessage(query.message!!)
        }

        answerCallbackQuery(
            query,
            "✅ Ваш отзыв о преподавателе #${teacherReviewId} был удален"
        )
    }

    answerCallbackQuery(query)
}