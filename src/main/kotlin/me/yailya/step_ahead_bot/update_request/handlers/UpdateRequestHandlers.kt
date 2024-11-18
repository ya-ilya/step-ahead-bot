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
import me.yailya.step_ahead_bot.update_request.UpdateRequest
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus
import me.yailya.step_ahead_bot.update_request.UpdateRequests
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun updateRequestForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<UpdateRequest?, UpdateRequest, UpdateRequest?> = databaseQuery {
    val (botUserEntity) = query.botUser()
    val condition = UpdateRequests.botUser eq botUserEntity.id
    val updateRequests = botUserEntity.updateRequests

    if (updateRequests.empty()) {
        throw RuntimeException("❌ Вы еще не создавали запросы на изменение информации")
    }

    val current = if (id == -1) {
        updateRequests.first()
    } else {
        UpdateRequestEntity.findById(id)
            ?: throw RuntimeException("❌ Данный запрос на изменение не существует")
    }

    if (current.botUser.id != botUserEntity.id) {
        throw RuntimeException("❌ Данный запрос на изменение создали не вы")
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

suspend fun BehaviourContext.handleUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (previous, updateRequest, next) = try {
        updateRequestForKeyboard(query, updateRequestId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    val university = updateRequest.university

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
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "update_request_${previous.id}")

                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "update_request_${next.id}")
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