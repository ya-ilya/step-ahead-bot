package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.assistant.Assistant

private val sessions = mutableListOf<Long>()

suspend fun BehaviourContext.handleAssistantCommand(message: TextMessage) {
    val userId = message.chat.id.chatId.long

    sessions.add(userId)

    val history = mutableListOf<ChatMessage>()

    history.add(
        AiMessage.from("Как я могу вам помочь?")
    )

    reply(
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

        if (!sessions.contains(userId)) {
            reply(
                to = request,
                text = "❌ Данная сессия с ассистентом была отменена!"
            )

            break
        }

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
        }
    }
}

fun removeSession(id: Long): Boolean {
    return sessions.remove(id)
}