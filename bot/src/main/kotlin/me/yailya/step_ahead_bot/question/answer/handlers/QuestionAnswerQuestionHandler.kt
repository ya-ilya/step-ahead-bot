package me.yailya.step_ahead_bot.question.answer.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.answer.QuestionAnswerEntity
import me.yailya.step_ahead_bot.reply

suspend fun BehaviourContext.handleAnswerQuestionCallback(
    query: DataCallbackQuery,
    answerId: Int
) {
    val (otherBotUser) = query.botUser()

    databaseQuery {
        val answer = QuestionAnswerEntity.findById(answerId)

        if (answer == null) {
            answerCallbackQuery(
                query,
                "❌ Данного ответа на вопрос не существует",
                showAlert = true
            )

            return@databaseQuery
        }

        if (answer.botUser.id != otherBotUser.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете посмотреть вопрос, ответ на который дали не вы",
                showAlert = true
            )

            return@databaseQuery
        }

        val question = answer.question

        reply(
            to = query,
            entities = buildEntities {
                +bold("${question.university.shortName} -> Вопрос #${question.id}") +
                        "\n" + question.text
            },
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("Посмотреть все ответы на этот вопрос", "Question_QuestionAnswers_${question.id}")
                }
            }
        )
    }

    answerCallbackQuery(query)
}