package me.yailya.step_ahead_bot.question.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.answer.Answer
import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.answer.Answers
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.replyOrEdit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun answerForKeyboard(
    id: Int,
    questionId: Int
): Triple<Answer?, Answer, Answer?> = databaseQuery {
    val condition = Answers.question eq questionId
    val question = QuestionEntity.findById(questionId) ?: throw RuntimeException("❌ Данный вопрос не существует")
    val answers = question.answers

    if (answers.empty()) {
        throw RuntimeException("❌ Ответов на этот вопрос нет")
    }

    val current = if (id == -1) {
        answers.first()
    } else {
        AnswerEntity.findById(id) ?: throw RuntimeException("❌ Данного ответа на вопрос не существует")
    }

    val previous = AnswerEntity
        .find { condition and (Answers.id less current.id) }
        .lastOrNull()
    val next = AnswerEntity
        .find { condition and (Answers.id greater current.id) }
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
                        "question_accept_answer_${answer.id}_${questionId}"
                    )
                }
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "question_answer_${previous.id}_${questionId}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "question_answer_${next.id}_${questionId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}