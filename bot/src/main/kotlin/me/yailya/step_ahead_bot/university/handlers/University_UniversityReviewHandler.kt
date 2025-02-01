package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.review.UniversityReview
import me.yailya.step_ahead_bot.university.review.UniversityReviewEntity
import me.yailya.step_ahead_bot.university.review.UniversityReviews
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun universityReviewForKeyboard(
    id: Int,
    university: University
): Triple<UniversityReview?, UniversityReview, UniversityReview?> = databaseQuery {
    val condition = UniversityReviews.university eq university.id
    val reviews = UniversityReviewEntity.find(condition)

    if (reviews.empty()) {
        throw RuntimeException("❌ Отзывов о ${university.shortName} не найдено")
    }

    val current = if (id == -1) {
        reviews.first()
    } else {
        UniversityReviewEntity.findById(id)
            ?: throw RuntimeException("❌ Данного отзыва о вузе не существует, либо же он был оставлен о другом ВУЗе")
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

suspend fun BehaviourContext.universityHandleUniversityReviewCallback(
    query: DataCallbackQuery,
    reviewId: Int,
    university: University
) {
    val (previous, review, next) = try {
        universityReviewForKeyboard(reviewId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message, showAlert = true)
        return
    }

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
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_UniversityReview_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_UniversityReview_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}