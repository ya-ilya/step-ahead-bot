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
import me.yailya.step_ahead_bot.assistant.AssistantQueue
import me.yailya.step_ahead_bot.editInlineButton

private val sessions = mutableSetOf<Long>()
private val queue = AssistantQueue(maxSize = System.getenv("ASSISTANT_QUEUE_MAX_SIZE").toInt())

suspend fun BehaviourContext.handleAssistantCommand(message: TextMessage) {
    val userId = message.chat.id.chatId.long

    sessions.add(userId)

    val history = mutableListOf<ChatMessage>(AiMessage.from(Assistant.defaultPrompt))

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

        if (queue.isFull()) {
            val text = { "⏳ Ожидание в очереди (ваше место: ${queue.getUserPosition(userId)}..." }
            val queueMessage = reply(
                to = request,
                text = text()
            )

            runBlocking {
                while (sessions.contains(userId) && queue.isFull()) {
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
            queue.addUser(userId)
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
            queue.removeUser(userId)
        }
    }
}

fun removeSession(id: Long): Boolean {
    return sessions.remove(id)
}