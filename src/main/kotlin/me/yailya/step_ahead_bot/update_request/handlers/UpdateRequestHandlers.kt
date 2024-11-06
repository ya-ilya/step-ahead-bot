package me.yailya.step_ahead_bot.update_request.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun BehaviourContext.handleUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val updateRequests = databaseQuery { query.botUser().first.updateRequests.map { it.toModel() } }

    if (updateRequests.isEmpty()) {
        answerCallbackQuery(
            query,
            "❌ Вы еще не создавали запросы на изменение информации"
        )

        return
    }

    val realUpdateRequestId = if (updateRequestId == -1) updateRequests.first().id else updateRequestId
    val updateRequest = updateRequests.find { it.id == realUpdateRequestId }

    if (updateRequest == null) {
        answerCallbackQuery(
            query,
            "❌ Данный запрос на изменение не существует, либо же его создали не вы"
        )

        return
    }

    val updateRequestIndex = updateRequests.indexOf(updateRequest)
    val previousUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex - 1).let { it?.id ?: -1 }
    val nextUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex + 1).let { it?.id ?: -1 }

    val university = Universities[updateRequest.universityId]

    replyOrEdit(
        updateRequestId == -1,
        query,
        buildEntities {
            +"\n" + "${university.shortName} -> Запрос на изменение информации #${updateRequest.id}\n- Статус: ${updateRequest.status.text}"

            if (updateRequest.moderator != null && updateRequest.commentFromModeration != null) {
                +"\n- Комментарий от модератора #${updateRequest.moderator.id}: " + blockquote(updateRequest.commentFromModeration)
            }

            +"\nИнформация, которую пользователь бы хотел поменять: " + blockquote(updateRequest.text)
        },
        inlineKeyboard {
            if (updateRequest.status == UpdateRequestStatus.Open) {
                row {
                    dataButton("❌ Закрыть", "update_request_close_${updateRequest.id}")
                }
            }
            row {
                if (previousUpdateRequestId != -1) {
                    dataButton("⬅\uFE0F Предыдущий", "update_request_${previousUpdateRequestId}")

                }
                if (nextUpdateRequestId != -1) {
                    dataButton("Следущий ➡\uFE0F", "update_request_${nextUpdateRequestId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
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

    answerCallbackQuery(query)
}