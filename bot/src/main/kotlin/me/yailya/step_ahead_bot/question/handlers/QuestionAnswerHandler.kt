package me.yailya.step_ahead_bot.question.handlers

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
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.answer.QuestionAnswer
import me.yailya.step_ahead_bot.question.answer.QuestionAnswerEntity
import me.yailya.step_ahead_bot.question.answer.QuestionAnswers
import me.yailya.step_ahead_bot.replyOrEdit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun answerForKeyboard(
    id: Int,
    questionId: Int
): Triple<QuestionAnswer?, QuestionAnswer, QuestionAnswer?> = databaseQuery {
    val condition = QuestionAnswers.question eq questionId
    val question = QuestionEntity.findById(questionId) ?: throw RuntimeException("❌ Данный вопрос не существует")
    val answers = question.answers

    if (answers.empty()) {
        throw RuntimeException("❌ Ответов на этот вопрос нет")
    }

    val current = if (id == -1) {
        answers.first()
    } else {
        QuestionAnswerEntity.findById(id) ?: throw RuntimeException("❌ Данного ответа на вопрос не существует")
    }

    val previous = QuestionAnswerEntity
        .find { condition and (QuestionAnswers.id less current.id) }
        .lastOrNull()
    val next = QuestionAnswerEntity
        .find { condition and (QuestionAnswers.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleQuestionAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int,
    questionId: Int
) {
    val (_, botUser) = query.botUser()
    val (previous, answer, next) = try {
        answerForKeyboard(answerId, questionId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        answerId == -1,
        query,
        buildEntities {
            +bold("Ответ на вопрос #${answer.id} о ${answer.question.university.shortName}") +
                    "\n" + answer.text
        },
        inlineKeyboard {
            row {
                if (answer.question.botUser.id == botUser.id) {
                    dataButton(
                        if (answer.isAccepted) "❌ Отменить одобрение" else "✅ Одобрить ответ",
                        "Question_accept_answer_${answer.id}_${questionId}"
                    )
                }
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "Question_QuestionAnswer_${previous.id}_${questionId}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "Question_QuestionAnswers_${next.id}_${questionId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}