@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.university.University
import java.time.LocalDateTime

suspend fun BehaviourContext.handleCreateQuestionCallback(
    query: DataCallbackQuery,
    university: University
) {
    val (botUserEntity, botUser) = query.botUser()

    if (botUser.lastQuestionTime != null && LocalDateTime.now() < botUser.lastQuestionTime.plusMinutes(1)) {
        answerCallbackQuery(
            query,
            "Вы должны подождать минуту, прежде чем задать новый вопрос"
        )

        return
    }

    val textMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("Создание вопроса о ${university.shortName}") +
                        "\n" + "Что вам хотелось бы узнать о данном ВУЗе?"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
        )
    ).first()

    val question = databaseQuery {
        botUserEntity.lastQuestionTime = LocalDateTime.now()

        QuestionEntity.new {
            this.botUser = botUserEntity
            this.universityId = university.id
            this.text = textMessage.content.text
        }.toModel()
    }

    reply(
        to = textMessage,
        text = "Вопрос об ${university.shortName} был задан! Номер вопроса: #${question.id}"
    )

    answerCallbackQuery(query)
}