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
import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.university.University

suspend fun BehaviourContext.notifyUserAboutQuestionAnswerCreated(entity: AnswerEntity) {
    val university = Universities[entity.question.universityId]
    send(
        ChatId(RawChatId(entity.question.botUser.userId)),
        buildEntities {
            +bold("На ваш вопрос о ${university.shortName} был дан новый ответ:") +
                    "\n" + entity.text
        }
    )
}

suspend fun BehaviourContext.handleCreateQuestionAnswerCallback(
    query: DataCallbackQuery,
    questionId: Int,
    university: University
) {
    val (botUserEntity) = query.botUser()
    val questionEntity = databaseQuery { QuestionEntity.findById(questionId) }

    if (questionEntity == null) {
        answerCallbackQuery(
            query,
            "Данного вопроса не существует"
        )

        return
    }

    val textMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("Создание ответа на вопрос (о ${university.shortName}) #${questionId}") +
                        "\n" + "Как вы хотите ответить на этот вопрос?"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
        )
    ).first()

    val answer = databaseQuery {
        AnswerEntity.new {
            this.botUser = botUserEntity
            this.question = questionEntity
            this.text = textMessage.content.text
        }.also { notifyUserAboutQuestionAnswerCreated(it) }.toModel()
    }

    reply(
        to = textMessage,
        text = "Ответ на вопрос (об ${university.shortName}) #${questionId} был задан! Номер ответа на вопрос: #${answer.id}"
    )

    answerCallbackQuery(query)
}