@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.question.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
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
import me.yailya.step_ahead_bot.answer.Answer
import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.answer.Answers
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.question.Question
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.replyOrEdit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun questionForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<Question?, Question, Question?> = databaseQuery {
    val (botUserEntity) = query.botUser()
    val condition = Questions.botUser eq botUserEntity.id
    val questions = botUserEntity.questions

    if (questions.empty()) {
        throw RuntimeException("❌ Вы еще не задавали вопросов о ВУЗах")
    }

    val current = if (id == -1) {
        questions.first()
    } else {
        QuestionEntity.findById(id) ?:
        throw RuntimeException("❌ Данного вопроса не существует")
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
                dataButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂\uFE0F Посмотреть ответы", "question_answers_${question.id}")
            }
            row {
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "question_delete_${question.id}")
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "question_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следущий ➡\uFE0F", "question_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleQuestionDeleteCallback(
    query: DataCallbackQuery,
    questionId: Int
) {
    val otherBotUser = query.botUser()

    databaseQuery {
        val question = QuestionEntity.findById(questionId)

        if (question == null) {
            answerCallbackQuery(
                query,
                "❌ Данного вопроса не существует"
            )

            return@databaseQuery
        }

        if (question.botUser.id != otherBotUser.first.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете удалить не ваш вопрос"
            )

            return@databaseQuery
        }

        question.delete()
        deleteMessage(query.message!!)

        answerCallbackQuery(
            query,
            "✅ Ваш вопрос #${questionId} был удален"
        )
    }
}

private suspend fun answerForKeyboard(
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
        AnswerEntity.findById(id) ?:
            throw RuntimeException("❌ Данного ответа на вопрос не существует")
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
                    dataButton("Следущий ➡\uFE0F", "question_answer_${next.id}_${questionId}")
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
    val (botUserEntity) = query.botUser()

    databaseQuery {
        val answer = AnswerEntity.findById(answerId)

        if (answer == null) {
            answerCallbackQuery(
                query,
                "❌ Данного ответа на вопрос не существует"
            )

            return@databaseQuery
        }

        val question = QuestionEntity.findById(questionId)

        if (question == null) {
            answerCallbackQuery(
                query,
                "❌ Данного вопроса не существует"
            )

            return@databaseQuery
        }

        if (question.botUser.id != botUserEntity.id) {
            answerCallbackQuery(
                query,
                "❌ Вы не можете одобрить ответ не на ваш вопрос"
            )

            return@databaseQuery
        }

        if (answer.question.id != question.id) {
            answerCallbackQuery(
                query,
                "❌ Данный ответ был дан на другой вопрос"
            )

            return@databaseQuery
        }

        if (!answer.isAccepted && question.answers.any { it.isAccepted }) {
            answerCallbackQuery(
                query,
                "❌ У данного вопроса уже есть принятый ответ"
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
                    if (answer.isAccepted) "Отменить одобрение" else "Одобрить",
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