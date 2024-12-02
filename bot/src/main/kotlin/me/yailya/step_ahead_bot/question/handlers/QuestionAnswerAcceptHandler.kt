package me.yailya.step_ahead_bot.question.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.answer.AnswerEntity


suspend fun BehaviourContext.notifyUserAboutAnswerAccepted(entity: AnswerEntity) {
    send(
        ChatId(RawChatId(entity.botUser.userId)),
        buildEntities {
            +bold("Ваш ответ на вопрос #${entity.id.value} был принят")
        }
    )
}

suspend fun BehaviourContext.handleQuestionAcceptAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int,
    questionId: Int
) {
    databaseQuery {
        val botUserEntity = query.botUser
        val answer = AnswerEntity.findById(answerId)

        if (answer == null) {
            answerCallbackQuery(
                query,
                "❌ Данного ответа на вопрос не существует"
            )

            return@databaseQuery
        }

        val question = QuestionEntity.findById(questionId)

        if (question == null) {
            answerCallbackQuery(
                query,
                "❌ Данного вопроса не существует"
            )

            return@databaseQuery
        }

        if (question.botUser.id != botUserEntity.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете одобрить ответ не на ваш вопрос"
            )

            return@databaseQuery
        }

        if (answer.question.id != question.id) {
            answerCallbackQuery(
                query,
                "❌ Данный ответ был дан на другой вопрос"
            )

            return@databaseQuery
        }

        if (!answer.isAccepted && question.answers.any { it.isAccepted }) {
            answerCallbackQuery(
                query,
                "❌ У данного вопроса уже есть принятый ответ"
            )

            return@databaseQuery
        }

        answer.isAccepted = !answer.isAccepted

        if (answer.isAccepted) {
            notifyUserAboutAnswerAccepted(answer)
        }

        editInlineButton(
            query,
            { button -> button.text.contains("Отменить одобрение") || button.text.contains("Одобрить ответ") },
            {
                CallbackDataInlineKeyboardButton(
                    if (answer.isAccepted) "❌ Отменить одобрение" else "✅ Одобрить ответ",
                    "question_accept_answer_${answer.id}_${questionId}"
                )
            }
        )

        answerCallbackQuery(
            query,
            if (answer.isAccepted) {
                "✅ Данный ответ на вопрос был помечен как принятый"
            } else {
                "❌ Данный ответ на вопрос перестал быть помеченным, как принятый"
            }
        )
    }
}