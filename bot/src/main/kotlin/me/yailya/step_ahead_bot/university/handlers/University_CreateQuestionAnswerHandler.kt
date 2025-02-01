@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.answer.QuestionAnswerEntity
import me.yailya.step_ahead_bot.university.University
import java.time.LocalDateTime

suspend fun BehaviourContext.notifyUserAboutQuestionAnswerCreated(entity: QuestionAnswerEntity) {
    val university = entity.question.university
    send(
        ChatId(RawChatId(entity.question.botUser.userId)),
        buildEntities {
            +bold("На ваш вопрос о ${university.shortName} был дан новый ответ:") +
                    "\n" + entity.text
        }
    )
}

suspend fun BehaviourContext.universityHandleCreateQuestionAnswerCallback(
    query: DataCallbackQuery,
    questionId: Int,
    university: University
) {
    val (botUserEntity, botUser) = query.botUser()
    val questionEntity = databaseQuery { QuestionEntity.findById(questionId) }

    if (questionEntity == null) {
        answerCallbackQuery(
            query,
            "❌ Данного вопроса не существует", showAlert = true
        )

        return
    }

    if (!botUser.isAdministrator) {
        if (databaseQuery { questionEntity.botUser.id == botUserEntity.id }) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете оставлять ответы на свой же вопрос", showAlert = true
            )

            return
        }
    }

    if (botUser.lastQuestionAnswerTime != null && LocalDateTime.now() < botUser.lastQuestionAnswerTime.plusMinutes(1)) {
        answerCallbackQuery(
            query,
            "⏳ Вы должны подождать минуту, прежде чем оставить новый ответ вопрос", showAlert = true
        )

        return
    }

    val textMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("${university.shortName} -> Вопрос #${questionId} -> Создание ответа на вопрос") +
                        "\n" + "Как вы хотите ответить на этот вопрос?"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
        )
    ).first()

    val answer = databaseQuery {
        botUserEntity.lastQuestionAnswerTime = LocalDateTime.now()

        QuestionAnswerEntity.new {
            this.botUser = botUserEntity
            this.question = questionEntity
            this.text = textMessage.content.text
        }.also { notifyUserAboutQuestionAnswerCreated(it) }.toModel()
    }

    reply(
        to = textMessage,
        text = "✅ Ответ на вопрос (об ${university.shortName}) #${questionId} был оставлен! Номер ответа на вопрос: #${answer.id}"
    )

    answerCallbackQuery(query)
}