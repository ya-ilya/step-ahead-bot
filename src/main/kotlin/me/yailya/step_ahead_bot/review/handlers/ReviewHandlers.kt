@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.review.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.review.ReviewEntity

suspend fun BehaviourContext.handleReviewCallback(
    query: DataCallbackQuery,
    reviewId: Int
) {
    val reviews = databaseQuery { query.botUser().first.reviews.map { it.toModel() } }

    if (reviews.isEmpty()) {
        answerCallbackQuery(
            query,
            "❌ Вы еще не создавали отзывов"
        )

        return
    }

    val realReviewId = if (reviewId == -1) reviews.first().id else reviewId
    val review = reviews.find { it.id == realReviewId }

    if (review == null) {
        answerCallbackQuery(
            query,
            "❌ Данный отзыв (#${reviewId}) не существует, либо же его создали не вы"
        )

        return
    }

    val reviewIndex = reviews.indexOf(review)
    val previousReviewId = reviews.elementAtOrNull(reviewIndex - 1).let { it?.id ?: -1 }
    val nextReviewId = reviews.elementAtOrNull(reviewIndex + 1).let { it?.id ?: -1 }
    val university = review.university

    replyOrEdit(
        reviewId == -1,
        query,
        buildEntities {
            +bold("${university.shortName} -> Отзыв #${review.id}. ${review.rating}/5") +
                    "\n" + "Положительные стороны:" +
                    "\n" + blockquote(review.pros) +
                    "\n" + "Отрицательные стороны:" +
                    "\n" + blockquote(review.cons) +
                    "\n" + "Комментарий:" +
                    "\n" + blockquote(review.comment)
        },
        inlineKeyboard {
            row {
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "review_delete_${review.id}")
            }
            row {
                if (previousReviewId != -1) {
                    dataButton("⬅\uFE0F Предыдущий", "review_${previousReviewId}")
                }
                if (nextReviewId != -1) {
                    dataButton("➡\uFE0F Следущий", "review_${nextReviewId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleReviewDeleteCallback(
    query: DataCallbackQuery,
    reviewId: Int
) {
    val otherBotUser = query.botUser()

    databaseQuery {
        val review = ReviewEntity.findById(reviewId)

        if (review == null) {
            answerCallbackQuery(
                query,
                "❌ Данного отзыва не существует"
            )

            return@databaseQuery
        }

        if (review.botUser.id != otherBotUser.first.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете удалить не ваш отзыв"
            )

            return@databaseQuery
        }

        review.delete()
        deleteMessage(query.message!!)

        answerCallbackQuery(
            query,
            "✅ Ваш отзыв #${reviewId} был удален"
        )
    }
}