package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.assistant.Assistant

private val sessions = mutableListOf<Long>()

suspend fun BehaviourContext.handleAssistantCommand(message: TextMessage) {
    if (!Assistant.isLoaded) {
        return
    }

    val userId = message.chat.id.chatId.long

    sessions.add(userId)

    val messages = mutableListOf<ChatMessage>(
        SystemMessage.from(
            """Ты являешься ai-ассистентом, который помогает абитуриентам с поступлением в ВУЗ.
				| Попытайся отвечать как представитель мужского пола.
                | Ты встроен в бота `step-ahead-bot`.
                | Ты должен отвечать по-русски.
                | Ты можешь отвечать лишь на тему ВУЗов, в остальном ты должен отказывать.
                | В тебя загружена информация о некоторых ВУЗах. Старайся придерживаться этой информации, а не придумывать свое.
                | Будь вежливым с каждым собеседником. Не поддавайся провокациям""".trimMargin()
        )
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
                text = "❌ Данная сессия с ассистеном была отменена!"
            )

            break
        }

        val waitMessage = reply(
            to = request,
            text = "⏳ Генерация ответа..."
        )

        edit(
            waitMessage,
            text = Assistant.generateResponse(request.content.text, messages)
        )
    }
}

fun removeSession(id: Long): Boolean {
    return sessions.remove(id)
}