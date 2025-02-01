package me.yailya.step_ahead_bot.university.review.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.review.UniversityReview
import me.yailya.step_ahead_bot.university.review.UniversityReviewEntity
import me.yailya.step_ahead_bot.university.review.UniversityReviews
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun universityReviewForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<UniversityReview?, UniversityReview, UniversityReview?> = databaseQuery {
    val botUserEntity = query.botUser
    val condition = UniversityReviews.botUser eq botUserEntity.id
    val reviews = botUserEntity.reviews

    if (reviews.empty()) {
        throw RuntimeException("❌ Вы еще не создавали отзывов")
    }

    val current = if (id == -1) {
        reviews.first()
    } else {
        UniversityReviewEntity.findById(id) ?: throw RuntimeException("❌ Данный отзыв не существует")
    }

    if (current.botUser.id != botUserEntity.id) {
        throw RuntimeException("❌ Данный отзыв создали не вы")
    }

    val previous = UniversityReviewEntity
        .find { condition and (UniversityReviews.id less current.id) }
        .lastOrNull()
    val next = UniversityReviewEntity
        .find { condition and (UniversityReviews.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleUniversityReviewCallback(
    query: DataCallbackQuery,
    reviewId: Int
) {
    val (previous, review, next) = try {
        universityReviewForKeyboard(query, reviewId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message, showAlert = true)
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
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "UniversityReview_delete_${review.id}")
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

    answerCallbackQuery(query)
}