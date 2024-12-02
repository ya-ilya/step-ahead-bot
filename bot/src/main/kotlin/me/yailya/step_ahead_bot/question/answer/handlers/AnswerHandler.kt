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
import me.yailya.step_ahead_bot.question.answer.Answer
import me.yailya.step_ahead_bot.question.answer.AnswerEntity
import me.yailya.step_ahead_bot.question.answer.Answers
import me.yailya.step_ahead_bot.replyOrEdit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun answerForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<Answer?, Answer, Answer?> = databaseQuery {
    val botUserEntity = query.botUser
    val condition = Answers.botUser eq botUserEntity.id
    val answers = botUserEntity.answers

    if (answers.empty()) {
        throw RuntimeException("❌ Вы еще не оставляли ответы на вопросы")
    }

    val current = if (id == -1) {
        answers.first()
    } else {
        AnswerEntity.findById(id) ?: throw RuntimeException("❌ Данного ответа на вопрос не существует")
    }

    if (current.botUser.id != botUserEntity.id) {
        throw RuntimeException("❌ Данный ответ на вопрос создали не вы")
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

suspend fun BehaviourContext.handleAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int
) {
    val (previous, answer, next) = try {
        answerForKeyboard(query, answerId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        answerId == -1,
        query,
        buildEntities {
            +bold("Ответ на вопрос #${answer.id}${if (answer.isAccepted) ". Помечен, как одобренный" else ""}") +
                    "\n" + answer.text
        },
        inlineKeyboard {
            row {
                dataButton("❔ Посмотреть вопрос", "Answer_question_${answer.id}")
            }
            row {
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "Answer_delete_${answer.id}")
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "Answer_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "Answer_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}