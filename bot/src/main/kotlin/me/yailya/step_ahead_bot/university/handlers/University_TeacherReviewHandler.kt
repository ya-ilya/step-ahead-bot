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
import me.yailya.step_ahead_bot.teacher.TeacherEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReview
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import me.yailya.step_ahead_bot.university.University
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun teacherReviewForKeyboard(
    id: Int,
    teacherId: Int,
    university: University
): Triple<TeacherReview?, TeacherReview, TeacherReview?> = databaseQuery {
    val condition = TeacherReviews.teacher eq teacherId
    val teacher = TeacherEntity.findById(teacherId) ?: throw RuntimeException("❌ Данный преподаватель не найден")

    if (teacher.university.id.value != university.id) {
        throw RuntimeException("❌ Данный преподаватель работает в другом ВУЗе")
    }

    val teacherReviews = teacher.reviews

    if (teacherReviews.empty()) {
        throw RuntimeException("❌ Отзывов о данном преподавателе не найдено")
    }

    val current = if (id == -1) {
        teacherReviews.first()
    } else {
        TeacherReviewEntity.findById(id) ?: throw RuntimeException("❌ Данный отзыв о преподавателе не найден")
    }

    val previous = TeacherReviewEntity
        .find { condition and (TeacherReviews.id less current.id) }
        .lastOrNull()
    val next = TeacherReviewEntity
        .find { condition and (TeacherReviews.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.universityHandleTeacherReviewCallback(
    query: DataCallbackQuery,
    teacherReviewId: Int,
    teacherId: Int,
    university: University
) {
    val (previous, teacherReview, next) = try {
        teacherReviewForKeyboard(teacherReviewId, teacherId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        teacherReviewId == -1,
        query,
        buildEntities {
            +bold("Отзыв о преподавателе #${teacherReview.id}. ${teacherReview.rating}/5") +
                    "\n" + blockquote(teacherReview.comment)
        },
        inlineKeyboard {
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_TeacherReview_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_TeacherReview_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}