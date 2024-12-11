@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.teacher.TeacherEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity
import me.yailya.step_ahead_bot.university.University
import java.time.LocalDateTime

suspend fun BehaviourContext.universityHandleCreateTeacherReviewCallback(
    query: DataCallbackQuery,
    teacherId: Int,
    university: University
) {
    val (botUserEntity, botUser) = query.botUser()

    if (botUser.lastTeacherReviewTime != null && LocalDateTime.now() < botUser.lastTeacherReviewTime.plusMinutes(1)) {
        answerCallbackQuery(
            query,
            "⏳ Вы должны подождать минуту, прежде чем оставить новый отзыв о преподавателе"
        )

        return
    }

    val (teacherEntity, teacherFullName) = databaseQuery {
        TeacherEntity.findById(teacherId)!!.let { it to it.toModel().fullName }
    }

    val commentMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("${university.shortName} -> Преподаватель $teacherFullName -> Создание отзыва") +
                        "\n" + "Что вы хотите рассказать об этом преподавателе?"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
        )
    ).first()

    val ratingQuery = waitDataCallbackQuery(
        SendTextMessage(
            query.message!!.chat.id,
            "Поставьте оценку данному преподавателю",
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("1\uFE0F⃣", "1")
                    dataButton("2\uFE0F⃣", "2")
                }
                row {
                    dataButton("3\uFE0F⃣", "3")
                    dataButton("4\uFE0F⃣", "4")
                }
                row {
                    dataButton("5\uFE0F⃣", "5")
                }
            }
        )
    ).first()

    answerCallbackQuery(ratingQuery)

    editInlineButton(
        ratingQuery,
        { button -> button.text.contains(ratingQuery.data) },
        { button -> dataInlineButton("✅ ${button.text}", ratingQuery.data) }
    )

    val rating = ratingQuery.data.toInt()

    val teacherReview = databaseQuery {
        botUserEntity.lastTeacherReviewTime = LocalDateTime.now()

        TeacherReviewEntity.new {
            this.botUser = botUserEntity
            this.teacher = teacherEntity
            this.comment = commentMessage.content.text
            this.rating = rating
        }.toModel()
    }

    reply(
        to = commentMessage,
        text = "✅ Спасибо за ваш отзыв о преподавателе ${teacherFullName}! Номер отзыва: #${teacherReview.id}"
    )

    answerCallbackQuery(query)
}