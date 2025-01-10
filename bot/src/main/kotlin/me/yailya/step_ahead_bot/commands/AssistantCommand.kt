package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.updates.hasCommands
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.yailya.step_ahead_bot.assistant.Assistant
import me.yailya.step_ahead_bot.editInlineButton

private const val MAX_QUEUE_SIZE = 1

private val sessions = mutableSetOf<Long>()
private val queue = mutableSetOf<Long>()

suspend fun BehaviourContext.handleAssistantCommand(message: TextMessage) {
    val userId = message.chat.id.chatId.long

    sessions.add(userId)

    val history = mutableListOf<ChatMessage>()

    history.add(
        AiMessage.from("Как я могу вам помочь?")
    )

    val initialMessage = reply(
        to = message,
        text = "Как я могу вам помочь?",
        replyMarkup = inlineKeyboard {
            row {
                dataButton("❌ Остановить ассистента", "assistant_stop_${userId}")
            }
        }
    )

    while (sessions.contains(userId)) {
        val request = waitTextMessage().first()

        if (request.hasCommands()) {
            removeSession(userId)
            editInlineButton(
                initialMessage,
                { button -> button.text.contains("Остановить ассистента") },
                null
            )
            break
        }

        if (!sessions.contains(userId)) {
            reply(
                to = request,
                text = "❌ Данная сессия с ассистентом уже была отменена"
            )

            break
        }

        if (queue.size == MAX_QUEUE_SIZE) {
            val text = { "⏳ Ожидание в очереди (ваше место: ${queue.indexOf(userId) + 1})..." }
            val queueMessage = reply(
                to = request,
                text = text()
            )

            runBlocking {
                while (sessions.contains(userId) && queue.size == MAX_QUEUE_SIZE) {
                    if (!queueMessage.content.text.contentEquals(text())) {
                        editMessageText(
                            queueMessage,
                            text()
                        )
                    }
                    delay(3000)
                }

                deleteMessage(queueMessage)
            }
        } else {
            queue.add(userId)
        }

        if (!sessions.contains(userId)) {
            reply(
                to = request,
                text = "❌ Данная сессия с ассистентом уже была отменена"
            )

            break
        }

        try {
            val waitMessage = reply(
                to = request,
                text = "⏳ Генерация ответа..."
            )

            val response = Assistant.generateResponse(request.content.text, history)

            if (response != null) {
                edit(
                    waitMessage,
                    text = response
                )
            } else {
                removeSession(userId)
                edit(
                    waitMessage,
                    text = "❌ Произошла ошибка при генерации ответа. Попробуйте еще раз, или свяжитесь с администрацией"
                )
                editInlineButton(
                    initialMessage,
                    { button -> button.text.contains("Остановить ассистента") },
                    null
                )
            }
        } finally {
            queue.remove(userId)
        }
    }
}

fun removeSession(id: Long): Boolean {
    return sessions.remove(id)
}