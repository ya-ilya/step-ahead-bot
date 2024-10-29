@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.moderator.handlers

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.moderator.ModeratorEntity
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun BehaviourContext.handleModerateUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
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
    val updateRequest = updateRequests.find { it.id == realUpdateRequestId }!!
    val updateRequestIndex = updateRequests.indexOf(updateRequest)
    val previousUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex - 1).let { it?.id ?: -1 }
    val nextUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex + 1).let { it?.id ?: -1 }

    reply(
        to = query,
        buildEntities {
            +"[Запрос №${updateRequest.id}]\n- Университет: ${Universities[updateRequest.universityId].name}\n- Статус: ${updateRequest.status.text}" +
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

suspend fun BehaviourContext.handleModerateUpdateRequestCloseCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val commentMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("Закрытие запроса #${updateRequestId} без пометки о выполнении") +
                        "\n" + "Прокомментируйте закрытие запроса:"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
        )
    ).first()

    databaseQuery {
        UpdateRequestEntity.findById(updateRequestId)!!.apply {
            this.status = UpdateRequestStatus.Closed
            this.commentFromModeration = commentMessage.content.text
        }
    }

    reply(
        to = commentMessage,
        "Запрос #${updateRequestId} успешно закрыт без пометки о выполнении"
    )
}

suspend fun BehaviourContext.handleModerateUpdateRequestCloseDoneCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val commentMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("Закрытие запроса #${updateRequestId} с пометкой о выполнении") +
                        "\n" + "Прокомментируйте закрытие запроса:"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
        )
    ).first()

    databaseQuery {
        UpdateRequestEntity.findById(updateRequestId)!!.apply {
            this.status = UpdateRequestStatus.ClosedAndDone
            this.moderatorId = ModeratorEntity.getModeratorByUserId(query.user.id.chatId.long)!!.id.value
            this.commentFromModeration = commentMessage.content.text

            send(
                ChatId(RawChatId(this.userId)),
                buildEntities {
                    +bold("Изменение статуса запроса #${id.value} о ${Universities[universityId].shortName}") +
                            "\n" + "- Статус изменен с ${UpdateRequestStatus.Open.text} на ${status.text}" +
                            "\n" + "- Комментарий от модератора ${moderatorId}:" +
                            "\n" + blockquote(commentFromModeration!!)
                }
            )
        }
    }

    reply(
        to = commentMessage,
        "Запрос #${updateRequestId} закрыт с пометкой о выполнении"
    )
}