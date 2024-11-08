@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.moderate_handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
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
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun BehaviourContext.handleModerateUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val updateRequests = UpdateRequestEntity.getModelsByStatus(UpdateRequestStatus.Open)

    if (updateRequests.isEmpty()) {
        answerCallbackQuery(
            query,
            "❌ Открытых запросов на изменение не найдено"
        )

        return
    }

    val realUpdateRequestId = if (updateRequestId == -1) updateRequests.first().id else updateRequestId
    val updateRequest = updateRequests.find { it.id == realUpdateRequestId }!!
    val updateRequestIndex = updateRequests.indexOf(updateRequest)
    val previousUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex - 1).let { it?.id ?: -1 }
    val nextUpdateRequestId = updateRequests.elementAtOrNull(updateRequestIndex + 1).let { it?.id ?: -1 }

    val university = updateRequest.university

    replyOrEdit(
        updateRequestId == -1,
        query,
        buildEntities {
            +"${university.shortName} -> Запрос на изменение информации #${updateRequest.id}]\n- Статус: ${updateRequest.status.text}" +
                    "\nИнформация, которую пользователь бы хотел поменять: " + blockquote(updateRequest.text)
        },
        inlineKeyboard {
            row {
                dataButton(
                    "❌ Закрыть, и пометить как выполненый",
                    "moderate_update_request_close_done_${updateRequest.id}"
                )
            }
            row {
                dataButton("❌ Закрыть без выполнения", "moderate_update_request_close_${updateRequest.id}")
            }
            row {
                if (previousUpdateRequestId != -1) {
                    dataButton("⬅\uFE0F Предыдущий", "moderate_update_request_${previousUpdateRequestId}")
                }
                if (nextUpdateRequestId != -1) {
                    dataButton("Следущий➡\uFE0F", "moderate_update_request_${nextUpdateRequestId}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.notifyUserAboutUpdateRequestClosed(entity: UpdateRequestEntity) {
    send(
        ChatId(RawChatId(entity.botUser.userId)),
        buildEntities {
            +bold("Изменение статуса запроса #${entity.id.value} о ${entity.university.shortName}") +
                    "\n" + "- Статус изменен с ${UpdateRequestStatus.Open.text} на ${entity.status.text}"

            if (entity.moderator != null && entity.commentFromModeration != null) {
                +"\n" + "- Комментарий от модератора #${entity.moderator!!.id}:" + "\n" + blockquote(entity.commentFromModeration!!)
            }
        }
    )
}

suspend fun BehaviourContext.checkUpdateRequestNotClosed(
    query: DataCallbackQuery,
    updateRequestId: Int
): Pair<Boolean, UpdateRequestEntity?> {
    val updateRequestEntity = databaseQuery { UpdateRequestEntity.findById(updateRequestId) }

    if (updateRequestEntity == null) {
        answerCallbackQuery(
            query,
            "❌ Этого запроса не существует"
        )

        return false to null
    }

    if (databaseQuery { updateRequestEntity.status } != UpdateRequestStatus.Open) {
        answerCallbackQuery(
            query,
            "❌ Этот запрос уже был закрыт"
        )

        return false to updateRequestEntity
    }

    return true to updateRequestEntity
}

suspend fun BehaviourContext.handleModerateUpdateRequestCloseCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (botUserEntity, _) = query.botUser()
    val (isCheckSuccessful, updateRequestEntity) = checkUpdateRequestNotClosed(query, updateRequestId)

    if (!isCheckSuccessful) {
        return
    }

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
        updateRequestEntity!!.apply {
            this.status = UpdateRequestStatus.Closed
            this.moderator = botUserEntity
            this.commentFromModeration = commentMessage.content.text

            notifyUserAboutUpdateRequestClosed(this)
        }
    }

    reply(
        to = commentMessage,
        "Запрос #${updateRequestId} успешно закрыт без пометки о выполнении"
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleModerateUpdateRequestCloseDoneCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (botUserEntity, _) = query.botUser()
    val (isCheckSuccessful, updateRequestEntity) = checkUpdateRequestNotClosed(query, updateRequestId)

    if (!isCheckSuccessful) {
        return
    }

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
        updateRequestEntity!!.apply {
            this.status = UpdateRequestStatus.ClosedAndDone
            this.moderator = botUserEntity
            this.commentFromModeration = commentMessage.content.text

            notifyUserAboutUpdateRequestClosed(this)
        }
    }

    reply(
        to = commentMessage,
        "Запрос #${updateRequestId} закрыт с пометкой о выполнении"
    )

    answerCallbackQuery(query)
}