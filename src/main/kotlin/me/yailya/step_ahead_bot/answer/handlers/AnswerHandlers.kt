@file:OptIn(RiskFeature::class, PreviewFeature::class)

package me.yailya.step_ahead_bot.answer.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.answer.Answer
import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.answer.Answers
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.edit
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.replyOrEdit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun answerForKeyboard(
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
                dataButton("❔ Посмотреть вопрос", "answer_question_${answer.id}")
            }
            row {
                dataButton("\uD83D\uDDD1\uFE0F Удалить", "answer_delete_${answer.id}")
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "answer_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "answer_${next.id}")
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

        reply(
            to = query,
            entities = buildEntities {
                +bold("${question.university.shortName} -> Вопрос #${question.id}") +
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

        databaseQuery {
            answer.delete()
        }

        try {
            val row = query
                .message!!
                .reply_markup!!
                .keyboard[2]

            val data = row
                .filterIsInstance<CallbackDataInlineKeyboardButton>()
                .first { it.text.contains("Следующий") || it.text.contains("Предыдущий") }
                .callbackData

            val otherId = data
                .split("_")
                .last()
                .toInt()

            val (previous, other, next) = answerForKeyboard(query, otherId)

            edit(
                query = query,
                entities = buildEntities {
                    +bold("Ответ на вопрос #${other.id}${if (other.isAccepted) ". Помечен, как одобренный" else ""}") +
                            "\n" + other.text
                },
                replyMarkup = inlineKeyboard {
                    row {
                        dataButton("❔ Посмотреть вопрос", "answer_question_${other.id}")
                    }
                    row {
                        dataButton("\uD83D\uDDD1\uFE0F Удалить", "answer_delete_${other.id}")
                    }
                    row {
                        if (previous != null) {
                            dataButton("⬅\uFE0F Предыдущий", "answer_${previous.id}")
                        }
                        if (next != null) {
                            dataButton("Следующий ➡\uFE0F", "answer_${next.id}")
                        }
                    }
                }
            )
        } catch (ex: Exception) {
            deleteMessage(query.message!!)
        }

        answerCallbackQuery(
            query,
            "✅ Ваш ответ на вопрос #${answerId} был удален"
        )
    }

    answerCallbackQuery(query)
}