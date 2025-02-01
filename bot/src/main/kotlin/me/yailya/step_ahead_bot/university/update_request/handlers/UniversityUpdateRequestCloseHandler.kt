package me.yailya.step_ahead_bot.university.update_request.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestStatus

suspend fun BehaviourContext.handleUniversityUpdateRequestCloseCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (isCheckSuccessful, updateRequestEntity) = isUpdateRequestMayClosed(query, updateRequestId)

    if (!isCheckSuccessful) {
        return
    }

    databaseQuery {
        updateRequestEntity!!.apply {
            this.status = UniversityUpdateRequestStatus.Closed
        }
    }

    editInlineButton(
        query,
        { button -> button.text.contains("Закрыть") },
        null
    )

    reply(
        to = query,
        text = "✅ Запрос на изменение информации #${updateRequestId} был успешно закрыт"
    )

    answerCallbackQuery(query)
}