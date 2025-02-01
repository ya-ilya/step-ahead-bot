package me.yailya.step_ahead_bot.teacher.review.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity

suspend fun BehaviourContext.handleTeacherReviewTeacherCallback(
    query: DataCallbackQuery,
    teacherReviewId: Int
) {
    val (otherBotUser) = query.botUser()

    databaseQuery {
        val teacherReview = TeacherReviewEntity.findById(teacherReviewId)

        if (teacherReview == null) {
            answerCallbackQuery(
                query,
                "❌ Данного отзыва о преподавателе не существует", showAlert = true
            )

            return@databaseQuery
        }

        if (teacherReview.botUser.id != otherBotUser.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете посмотреть информацию о преподавателе, отзыв на которого оставили не вы",
                showAlert = true
            )

            return@databaseQuery
        }

        val teacher = teacherReview.teacher

        reply(
            to = query,
            entities = buildEntities {
                +bold("${teacher.university.shortName} -> преподаватели -> ${teacher.fullName}") +
                        "\n- Опыт работы: ${teacher.experience}" +
                        "\n- Академическая должность: ${teacher.academicTitle}" +
                        "\n- Специальности: ${teacher.specialities.joinToString()}"
            }
        )
    }

    answerCallbackQuery(query)
}