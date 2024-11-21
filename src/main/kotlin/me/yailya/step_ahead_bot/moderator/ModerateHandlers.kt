@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.moderator

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
import me.yailya.step_ahead_bot.update_request.UpdateRequest
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus
import me.yailya.step_ahead_bot.update_request.UpdateRequests
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun updateRequestForKeyboard(id: Int): Triple<UpdateRequest?, UpdateRequest, UpdateRequest?> =
    databaseQuery {
        val condition = UpdateRequests.status eq UpdateRequestStatus.Open
        val updateRequests = UpdateRequestEntity.find(condition)

        if (updateRequests.empty()) {
            throw RuntimeException("❌ Открытых запросов на изменение не найдено")
        }

        val current = if (id == -1) {
            updateRequests.first()
        } else {
            UpdateRequestEntity.findById(id)
                ?: throw RuntimeException("❌ Данный запрос на изменение не существует")
        }

        val previous = UpdateRequestEntity
            .find { condition and (UpdateRequests.id less current.id) }
            .lastOrNull()
        val next = UpdateRequestEntity
            .find { condition and (UpdateRequests.id greater current.id) }
            .firstOrNull()

        return@databaseQuery Triple(
            previous?.toModel(),
            current.toModel(),
            next?.toModel()
        )
    }

suspend fun BehaviourContext.handleModerateUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (previous, updateRequest, next) = try {
        updateRequestForKeyboard(updateRequestId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

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
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "moderate_update_request_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий➡\uFE0F", "moderate_update_request_${next.id}")
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