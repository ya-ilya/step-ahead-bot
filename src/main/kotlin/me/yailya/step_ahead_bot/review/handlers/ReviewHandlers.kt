@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.review.handlers

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
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.review.Review
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.review.Reviews
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun reviewForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<Review?, Review, Review?> = databaseQuery {
    val botUserEntity = query.botUser
    val condition = Reviews.botUser eq botUserEntity.id
    val reviews = botUserEntity.reviews

    if (reviews.empty()) {
        throw RuntimeException("❌ Вы еще не создавали отзывов")
    }

    val current = if (id == -1) {
        reviews.first()
    } else {
        ReviewEntity.findById(id) ?: throw RuntimeException("❌ Данный отзыв не существует")
    }

    if (current.botUser.id != botUserEntity.id) {
        throw RuntimeException("❌ Данный отзыв создали не вы")
    }

    val previous = ReviewEntity
        .find { condition and (Reviews.id less current.id) }
        .lastOrNull()
    val next = ReviewEntity
        .find { condition and (Reviews.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleReviewCallback(
    query: DataCallbackQuery,
    reviewId: Int
) {
    val (previous, review, next) = try {
        reviewForKeyboard(query, reviewId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

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
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "review_${previous.id}")
                }
                if (next != null) {
                    dataButton("➡\uFE0F Следующий", "review_${next.id}")
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

            val (previous, other, next) = reviewForKeyboard(query, otherId)

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
                        dataButton("\uD83D\uDDD1\uFE0F Удалить", "review_delete_${other.id}")
                    }
                    row {
                        if (previous != null) {
                            dataButton("⬅\uFE0F Предыдущий", "review_${previous.id}")
                        }
                        if (next != null) {
                            dataButton("➡\uFE0F Следующий", "review_${next.id}")
                        }
                    }
                }
            )
        } catch (ex: Exception) {
            deleteMessage(query.message!!)
        }

        answerCallbackQuery(
            query,
            "✅ Ваш отзыв #${reviewId} был удален"
        )
    }
}