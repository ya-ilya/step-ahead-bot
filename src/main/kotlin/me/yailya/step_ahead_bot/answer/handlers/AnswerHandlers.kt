@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.answer.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.Universities

suspend fun BehaviourContext.handleAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int
) {
    val answers = databaseQuery { query.botUser().first.answers.map { it.toModel() } }

    if (answers.isEmpty()) {
        answerCallbackQuery(
            query,
            "❌ Вы еще не оставляли ответы на вопросы"
        )

        return
    }

    val realAnswerId = if (answerId != -1) answerId else answers.first().id
    val answer = answers.find { it.id == realAnswerId }

    if (answer == null) {
        answerCallbackQuery(
            query,
            "❌ Данного ответа на вопрос не существует, либо же его создали не вы"
        )

        return
    }

    val answerIndex = answers.indexOf(answer)
    val previousAnswerId = answers.elementAtOrNull(answerIndex - 1).let { it?.id ?: -1 }
    val nextAnswerId = answers.elementAtOrNull(answerIndex + 1).let { it?.id ?: -1 }

    replyOrEdit(
        answerId == -1,
        query,
        buildEntities {
            +bold("Ответ на вопрос #${answer.id}${if (answer.isAccepted) ". Помечен, как одобренный" else ""}") +
                    "\n" + answer.text
        },
        inlineKeyboard {
            row {
                dataButton("❔ Посмотреть вопрос", "answer_question_${answer.id}")
            }
            row {
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "answer_delete_${answer.id}")
            }
            row {
                if (previousAnswerId != -1) {
                    dataButton("⬅\uFE0F Предыдущий", "answer_${previousAnswerId}")
                }
                if (nextAnswerId != -1) {
                    dataButton("Следущий ➡\uFE0F", "answer_${nextAnswerId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleAnswerQuestionCallback(
    query: DataCallbackQuery,
    answerId: Int
) {
    val (otherBotUser) = query.botUser()

    databaseQuery {
        val answer = AnswerEntity.findById(answerId)

        if (answer == null) {
            answerCallbackQuery(
                query,
                "❌ Данного ответа на вопрос не существует"
            )

            return@databaseQuery
        }

        if (answer.botUser.id != otherBotUser.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете посмотреть вопрос, ответ на который дали не вы"
            )

            return@databaseQuery
        }

        val question = answer.question
        val university = Universities[question.universityId]

        reply(
            to = query,
            entities = buildEntities {
                +bold("${university.shortName} -> Вопрос #${question.id}") +
                        "\n" + question.text
            },
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("Посмотреть все ответы на этот вопрос", "question_answers_${question.id}")
                }
            }
        )
    }

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleAnswerDeleteCallback(
    query: DataCallbackQuery,
    answerId: Int
) {
    val (otherBotUser) = query.botUser()

    databaseQuery {
        val answer = AnswerEntity.findById(answerId)

        if (answer == null) {
            answerCallbackQuery(
                query,
                "❌ Данного ответа на вопрос не существует"
            )

            return@databaseQuery
        }

        if (answer.botUser.id != otherBotUser.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете удалить не ваш ответ на вопрос"
            )

            return@databaseQuery
        }

        answer.delete()
        deleteMessage(query.message!!)

        answerCallbackQuery(
            query,
            "✅ Ваш ответ на вопрос #${answerId} был удален"
        )
    }

    answerCallbackQuery(query)
}