@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.question.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.Universities

suspend fun BehaviourContext.handleQuestionCallback(
    query: DataCallbackQuery,
    questionId: Int
) {
    val questions = databaseQuery { query.botUser().first.questions.map { it.toModel() } }

    if (questions.isEmpty()) {
        answerCallbackQuery(
            query,
            "Вы еще не задавали вопросов о ВУЗах"
        )

        return
    }

    val realQuestionId = if (questionId == -1) questions.first().id else questionId
    val question = questions.find { it.id == realQuestionId }

    if (question == null) {
        answerCallbackQuery(
            query,
            "Данного вопроса не существует"
        )

        return
    }

    val questionIndex = questions.indexOf(question)
    val previousQuestionId = questions.elementAtOrNull(questionIndex - 1).let { it?.id ?: -1 }
    val nextQuestionId = questions.elementAtOrNull(questionIndex + 1).let { it?.id ?: -1 }

    replyOrEdit(
        questionId == -1,
        query,
        buildEntities {
            +bold("Вопрос #${question.id} об ${Universities[question.universityId].shortName}") +
                    "\n" + question.text
        },
        inlineKeyboard {
            row {
                dataButton("Посмотреть ответы на этот вопрос", "question_answers_${question.id}")
            }
            if (previousQuestionId != -1) {
                row {
                    dataButton("Предыдущий", "question_${previousQuestionId}")
                }
            }
            if (nextQuestionId != -1) {
                row {
                    dataButton("Следущий", "question_${nextQuestionId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleQuestionAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int,
    questionId: Int
) {
    val answers = databaseQuery { QuestionEntity.findById(questionId)!!.answers.map { it.toModel() } }

    if (answers.isEmpty()) {
        answerCallbackQuery(
            query,
            "Ответов на этот вопрос нет"
        )

        return
    }

    val realAnswerId = if (answerId != -1) answerId else answers.first().id
    val answer = answers.find { it.id == realAnswerId }

    if (answer == null) {
        answerCallbackQuery(
            query,
            "Данного ответа на вопрос не существует"
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
            row {
                dataButton(
                    if (answer.isAccepted) "Отменить одобрение" else "Одобрить ответ",
                    "question_accept_answer_${answer.id}_${questionId}"
                )
            }
            if (previousAnswerIndex != -1) {
                row {
                    dataButton("Предыдущий", "question_answer_${previousAnswerIndex}_${questionId}")
                }
            }
            if (nextAnswerIndex != -1) {
                row {
                    dataButton("Следущий", "question_answer_${nextAnswerIndex}_${questionId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

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
        val answer = AnswerEntity.findById(answerId)

        if (answer == null) {
            answerCallbackQuery(
                query,
                "Данного ответа на вопрос не существует"
            )

            return@databaseQuery
        }

        val question = QuestionEntity.findById(questionId)

        if (question == null) {
            answerCallbackQuery(
                query,
                "Данного вопроса не существует"
            )

            return@databaseQuery
        }

        if (answer.question.id != question.id) {
            answerCallbackQuery(
                query,
                "Данный ответ был дан на другой вопрос"
            )

            return@databaseQuery
        }

        if (!answer.isAccepted && question.answers.any { it.isAccepted }) {
            answerCallbackQuery(
                query,
                "У данного вопроса уже есть принятый ответ"
            )

            return@databaseQuery
        }

        answer.isAccepted = !answer.isAccepted

        if (answer.isAccepted) {
            notifyUserAboutAnswerAccepted(answer)
        }

        editInlineButton(
            query,
            { button -> button.text.contains("одобр", true) },
            {
                CallbackDataInlineKeyboardButton(
                    if (answer.isAccepted) "Отменить одобрение" else "Одобрить ответ",
                    "question_accept_answer_${answer.id}_${questionId}"
                )
            }
        )

        answerCallbackQuery(
            query,
            if (answer.isAccepted) {
                "Данный ответ на вопрос был помечен как принятый"
            } else {
                "Данный ответ на вопрос перестал быть помеченым, как принятый"
            }
        )
    }
}