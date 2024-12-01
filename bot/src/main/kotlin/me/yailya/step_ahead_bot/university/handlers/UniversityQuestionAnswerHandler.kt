package me.yailya.step_ahead_bot.university.handlers

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
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.University
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun answerForKeyboard(
    id: Int,
    questionId: Int,
    university: University
): Triple<Answer?, Answer, Answer?> = databaseQuery {
    val condition = Answers.question eq questionId
    val question = QuestionEntity.findById(questionId) ?: throw RuntimeException("❌ Данный вопрос не существует")

    if (question.university.id.value != university.id) {
        throw RuntimeException("❌ Данный вопрос был задан о другом ВУЗе")
    }

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

suspend fun BehaviourContext.handleUniversityQuestionAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int,
    questionId: Int,
    university: University
) {
    val (previous, answer, next) = try {
        answerForKeyboard(answerId, questionId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        answerId == -1,
        query,
        buildEntities {
            +bold("Ответ на вопрос о ${university.shortName} #${answer.id}") +
                    "\n" + answer.text
        },
        inlineKeyboard {
            row {
                if (previous != null) {
                    dataButton(
                        "⬅\uFE0F Предыдущий",
                        "university_question_answer_${previous.id}_${questionId}_${university.id}"
                    )
                }
                if (next != null) {
                    dataButton(
                        "Следующий ➡\uFE0F",
                        "university_question_answer_${next.id}_${questionId}_${university.id}"
                    )
                }
            }
        }
    )

    answerCallbackQuery(query)
}