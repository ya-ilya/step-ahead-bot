package me.yailya.step_ahead_bot.answer.handlers

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
import me.yailya.step_ahead_bot.replyOrEdit

suspend fun BehaviourContext.handleAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int
) {
    val answers = databaseQuery { query.botUser().first.answers.map { it.toModel() } }

    if (answers.isEmpty()) {
        answerCallbackQuery(
            query,
            "Вы еще не оставляли ответы на вопросы"
        )

        return
    }

    val realAnswerId = if (answerId != -1) answerId else answers.first().id
    val answer = answers.find { it.id == realAnswerId }

    if (answer == null) {
        answerCallbackQuery(
            query,
            "Данного ответа на вопрос не существует, либо же его создали не вы"
        )

        return
    }

    val answerIndex = answers.indexOf(answer)
    val previousAnswerIndex = answers.elementAtOrNull(answerIndex - 1).let { it?.id ?: -1 }
    val nextAnswerIndex = answers.elementAtOrNull(answerIndex + 1).let { it?.id ?: -1 }

    replyOrEdit(
        answerId == -1,
        query,
        buildEntities {
            +bold("Ответ на вопрос #${answer.id}") +
                    "\n" + answer.text
        },
        inlineKeyboard {
            if (previousAnswerIndex != -1) {
                row {
                    dataButton("Предыдущий", "my_question_answer_${previousAnswerIndex}")
                }
            }
            if (nextAnswerIndex != -1) {
                row {
                    dataButton("Следущий", "my_question_answer_${nextAnswerIndex}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}