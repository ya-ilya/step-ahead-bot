package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.assistant.Assistant

suspend fun BehaviourContext.handleAssistantCommand(message: TextMessage) {
    if (!Assistant.isLoaded) {
        return
    }

    val messages = mutableListOf<ChatMessage>(
        SystemMessage.from(
            """Ты являешься ai-ассистентом, который помогает абитуриентам с поступлением в ВУЗ.
                | Ты встроен в бота step-ahead-bot.
                | Ты должен отвечать по-русски.
                | Ты можешь отвечать лишь на тему ВУЗов, в остальном ты должен отказывать.""".trimMargin()
        )
    )

    val request = waitText(
        SendTextMessage(
            chatId = message.chat.id,
            text = "Как я могу вам помочь?"
        )
    ).first()

    val response = Assistant.generateResponse(request.text, messages)

    reply(
        to = message,
        text = response
    )
}