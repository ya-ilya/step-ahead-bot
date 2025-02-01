@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.review.handlers

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
import me.yailya.step_ahead_bot.university.review.UniversityReviewEntity

suspend fun BehaviourContext.handleUniversityReviewDeleteCallback(
    query: DataCallbackQuery,
    reviewId: Int
) {
    val otherBotUser = query.botUser()

    databaseQuery {
        val review = UniversityReviewEntity.findById(reviewId)

        if (review == null) {
            answerCallbackQuery(
                query,
                "❌ Данного отзыва о вузе не существует", showAlert = true
            )

            return@databaseQuery
        }

        if (review.botUser.id != otherBotUser.first.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете удалить не ваш отзыв о вузе", showAlert = true
            )

            return@databaseQuery
        }

        databaseQuery {
            review.delete()
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

            val (previous, other, next) = universityReviewForKeyboard(query, otherId)

            edit(
                query = query,
                entities = buildEntities {
                    +bold("${other.university.shortName} -> Отзыв #${other.id}. ${other.rating}/5") +
                            "\n" + "Положительные стороны:" +
                            "\n" + blockquote(other.pros) +
                            "\n" + "Отрицательные стороны:" +
                            "\n" + blockquote(other.cons) +
                            "\n" + "Комментарий:" +
                            "\n" + blockquote(other.comment)
                },
                replyMarkup = inlineKeyboard {
                    row {
                        dataButton("\uD83D\uDDD1\uFE0F Удалить", "UniversityReview_delete_${other.id}")
                    }
                    row {
                        if (previous != null) {
                            dataButton("⬅\uFE0F Предыдущий", "UniversityReview_${previous.id}")
                        }
                        if (next != null) {
                            dataButton("➡\uFE0F Следующий", "UniversityReview_${next.id}")
                        }
                    }
                }
            )
        } catch (ex: Exception) {
            deleteMessage(query.message!!)
        }

        answerCallbackQuery(
            query,
            "✅ Ваш отзыв #${reviewId} был удален", showAlert = true
        )
    }
}