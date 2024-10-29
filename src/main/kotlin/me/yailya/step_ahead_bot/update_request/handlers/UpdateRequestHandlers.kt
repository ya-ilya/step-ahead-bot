@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.update_request.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun BehaviourContext.handleUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val updateRequests = UpdateRequestEntity.getModelsByUserId(query.user.id.chatId.long)

    if (updateRequests.isEmpty()) {
        reply(
            to = query,
            buildEntities {
                +bold("Вы еще не создавали запросы на изменение информации")
            }
        )

        return
    }

    val realUpdateRequestId = if (updateRequestId == -1) updateRequests.first().id else updateRequestId
    val updateRequest = updateRequests.find { it.id == realUpdateRequestId }!!
    val updateRequestIndex = updateRequests.indexOf(updateRequest)
    val previousUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex - 1).let { it?.id ?: -1 }
    val nextUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex + 1).let { it?.id ?: -1 }

    reply(
        to = query,
        buildEntities {
            val university = Universities[updateRequest.universityId]

            +"\n" + "[Запрос №${updateRequest.id}]\n- Университет: ${university.name}\n- Статус: ${updateRequest.status.text}"

            if (updateRequest.moderatorId != null && updateRequest.commentFromModeration != null) {
                +"\n- Комментарий от модератора #${updateRequest.moderatorId}: " + blockquote(updateRequest.commentFromModeration)
            }

            +"\nИнформация, которую пользователь бы хотел поменять: " + blockquote(updateRequest.text)
        },
        replyMarkup = inlineKeyboard {
            if (updateRequest.status == UpdateRequestStatus.Open) {
                row {
                    dataButton("Закрыть запрос", "update_request_close_${updateRequest.id}")
                }
            }
            if (previousUpdateRequestId != -1) {
                row {
                    dataButton("Предыдущий", "update_request_${previousUpdateRequestId}")
                }
            }
            if (nextUpdateRequestId != -1) {
                row {
                    dataButton("Следущий", "update_request_${nextUpdateRequestId}")
                }
            }
        }
    )
}

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
        text = "Запрос #${updateRequestId} был успешно закрыт"
    )
}