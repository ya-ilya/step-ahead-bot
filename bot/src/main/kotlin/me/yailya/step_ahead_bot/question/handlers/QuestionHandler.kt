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
import me.yailya.step_ahead_bot.question.Question
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.replyOrEdit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun questionForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<Question?, Question, Question?> = databaseQuery {
    val botUserEntity = query.botUser
    val condition = Questions.botUser eq botUserEntity.id
    val questions = botUserEntity.questions

    if (questions.empty()) {
        throw RuntimeException("❌ Вы еще не задавали вопросов о ВУЗах")
    }

    val current = if (id == -1) {
        questions.first()
    } else {
        QuestionEntity.findById(id) ?: throw RuntimeException("❌ Данного вопроса не существует")
    }

    if (current.botUser.id != botUserEntity.id) {
        throw RuntimeException("❌ Данный вопрос задали не вы")
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

suspend fun BehaviourContext.handleQuestionCallback(
    query: DataCallbackQuery,
    questionId: Int
) {
    val (previous, question, next) = try {
        questionForKeyboard(query, questionId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    val university = question.university

    replyOrEdit(
        questionId == -1,
        query,
        buildEntities {
            +bold("${university.shortName} -> Вопрос #${question.id}") +
                    "\n" + question.text
        },
        inlineKeyboard {
            row {
                dataButton(
                    "\uD83D\uDE4B\uD83C\uDFFB\u200D♂\uFE0F Посмотреть ответы",
                    "Question_QuestionAnswers_${question.id}"
                )
            }
            row {
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "Question_delete_${question.id}")
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "Question_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "Question_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}