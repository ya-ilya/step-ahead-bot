package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.Question
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.University
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun questionForKeyboard(
    id: Int,
    university: University
): Triple<Question?, Question, Question?> = databaseQuery {
    val condition = Questions.university eq university.id
    val questions = QuestionEntity.find(condition)

    if (questions.empty()) {
        throw RuntimeException("❌ Вопросов о ${university.shortName} не найдено")
    }

    val current = if (id == -1) {
        questions.first()
    } else {
        QuestionEntity.findById(id) ?: throw RuntimeException("❌ Данного вопроса не существует")
    }

    val previous = QuestionEntity
        .find { condition and (Questions.id less current.id) }
        .lastOrNull()
    val next = QuestionEntity
        .find { condition and (Questions.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleUniversityQuestionCallback(
    query: DataCallbackQuery,
    questionId: Int,
    university: University
) {
    val (previous, question, next) = try {
        questionForKeyboard(questionId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        questionId == -1,
        query,
        buildEntities {
            +bold("Вопрос #${question.id}") +
                    "\n" + question.text
        },
        inlineKeyboard {
            row {
                dataButton(
                    "✍\uD83C\uDFFB Оставить ответ",
                    "university_create_question_answer_${question.id}_${university.id}"
                )
            }
            row {
                dataButton(
                    "\uD83D\uDE4B\uD83C\uDFFB\u200D♂\uFE0F Посмотреть ответы",
                    "university_question_answers_${question.id}_${university.id}"
                )
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_question_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_question_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}