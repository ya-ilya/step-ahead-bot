package me.yailya.step_ahead_bot.teacher.review.handlers

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
import me.yailya.step_ahead_bot.teacher.review.TeacherReview
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun teacherReviewForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<TeacherReview?, TeacherReview, TeacherReview?> = databaseQuery {
    val botUserEntity = query.botUser
    val condition = TeacherReviews.botUser eq botUserEntity.id
    val teacherReviews = botUserEntity.teacherReviews

    if (teacherReviews.empty()) {
        throw RuntimeException("❌ Вы еще не создавали отзывов о преподавателях")
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

suspend fun BehaviourContext.handleTeacherReviewCallback(
    query: DataCallbackQuery,
    teacherReviewId: Int
) {
    val (previous, teacherReview, next) = try {
        teacherReviewForKeyboard(query, teacherReviewId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message, showAlert = true)
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
                dataButton(
                    "\uD83D\uDC69\u200D\uD83C\uDFEB Посмотреть информацию о преподавателе",
                    "TeacherReview_teacher_${teacherReview.id}"
                )
            }
            row {
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "TeacherReview_delete_${teacherReview.id}")
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "TeacherReview_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "TeacherReview_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}