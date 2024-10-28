@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.moderator.handlers

import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.update_request.UpdateRequest
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun BehaviourContext.sendUpdateRequestMessage(
    query: DataCallbackQuery,
    updateRequest: UpdateRequest,
    previousUpdateRequestId: Int,
    nextUpdateRequestId: Int
) {
    val university = Universities[updateRequest.universityId]

    reply(
        to = query,
        buildEntities {
            +"[Запрос №${updateRequest.id}]\n- Университет: ${university.name}\n- Статус: ${updateRequest.status.text}" +
                    "\nИнформация, которую пользователь бы хотел поменять: " + blockquote(updateRequest.text)
        },
        replyMarkup = inlineKeyboard {
            row {
                dataButton(
                    "Закрыть запрос, и пометить как выполненое",
                    "moderate_update_request_close_done_${updateRequest.id}"
                )
            }
            row {
                dataButton("Закрыть запрос без его выполнения", "moderate_update_request_close_${updateRequest.id}")
            }
            if (previousUpdateRequestId != -1) {
                row {
                    dataButton("Предыдущий", "moderate_update_request_${previousUpdateRequestId}")
                }
            }
            if (nextUpdateRequestId != -1) {
                row {
                    dataButton("Следущий", "moderate_update_request_${nextUpdateRequestId}")
                }
            }
        }
    )
}

suspend fun BehaviourContext.handleModerateUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int,
    previousMessageId: Long? = null
) {
    if (previousMessageId != null) {
        deleteMessage(query.message!!.chat, MessageId(previousMessageId))
    }

    val updateRequests = UpdateRequestEntity.getModelsByStatus(UpdateRequestStatus.Open)

    if (updateRequests.isEmpty()) {
        reply(
            to = query,
            buildEntities {
                +bold("Открытых запросов на изменение не найдено")
            }
        )

        return
    }

    val realUpdateRequestId = if (updateRequestId == -1) updateRequests.first().id else updateRequestId
    val currentElement = updateRequests.find { it.id == realUpdateRequestId }!!
    val currentElementId = updateRequests.indexOf(currentElement)
    val previousElement = updateRequests.elementAtOrNull(currentElementId - 1)
    val nextElement = updateRequests.elementAtOrNull(currentElementId + 1)

    sendUpdateRequestMessage(
        query,
        currentElement,
        previousElement?.id ?: -1,
        nextElement?.id ?: -1
    )
}