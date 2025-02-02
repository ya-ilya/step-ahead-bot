@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.assistant.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import me.yailya.step_ahead_bot.commands.removeSession
import me.yailya.step_ahead_bot.editInlineButton

suspend fun BehaviourContext.handleAssistantStop(query: DataCallbackQuery, userId: Long) {
    if (query.message!!.chat.id.chatId.long != userId) {
        answerCallbackQuery(query, "❌ Вы не можете остановить не свою сессию", showAlert = true)
    }

    if (removeSession(userId)) {
        editInlineButton(
            query,
            { button -> button.text.contains("Остановить") },
            null
        )

        answerCallbackQuery(query, "✅ Ваша сессия с ассистентом была успешно остановлена", showAlert = true)
    } else {
        answerCallbackQuery(query, "❌ У вас нет активной сессии с ассистентом", showAlert = true)
    }
}