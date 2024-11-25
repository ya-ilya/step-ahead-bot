package me.yailya.step_ahead_bot.update_request.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun BehaviourContext.handleUpdateRequestCloseCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    databaseQuery {
        UpdateRequestEntity.findById(updateRequestId)!!.apply {
            this.status = UpdateRequestStatus.Closed
        }
    }

    reply(
        to = query,
        text = "Запрос на изменение информации #${updateRequestId} был успешно закрыт"
    )

    answerCallbackQuery(query)
}