@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.moderator

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequest
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestEntity
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestStatus
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequests
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun updateRequestForKeyboard(id: Int): Triple<UniversityUpdateRequest?, UniversityUpdateRequest, UniversityUpdateRequest?> =
    databaseQuery {
        val condition = UniversityUpdateRequests.status eq UniversityUpdateRequestStatus.Open
        val updateRequests = UniversityUpdateRequestEntity.find(condition)

        if (updateRequests.empty()) {
            throw RuntimeException("❌ Открытых запросов на изменение не найдено")
        }

        val current = if (id == -1) {
            updateRequests.first()
        } else {
            UniversityUpdateRequestEntity.findById(id)
                ?: throw RuntimeException("❌ Данный запрос на изменение не существует")
        }

        val previous = UniversityUpdateRequestEntity
            .find { condition and (UniversityUpdateRequests.id less current.id) }
            .lastOrNull()
        val next = UniversityUpdateRequestEntity
            .find { condition and (UniversityUpdateRequests.id greater current.id) }
            .firstOrNull()

        return@databaseQuery Triple(
            previous?.toModel(),
            current.toModel(),
            next?.toModel()
        )
    }

suspend fun BehaviourContext.moderateHandleUniversityUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (previous, updateRequest, next) = try {
        updateRequestForKeyboard(updateRequestId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message, showAlert = true)
        return
    }

    val university = updateRequest.university

    replyOrEdit(
        updateRequestId == -1,
        query,
        buildEntities {
            +"${university.shortName} -> Запрос на изменение информации #${updateRequest.id}]" +
                    "\nИнформация, которую пользователь бы хотел поменять: " + blockquote(updateRequest.text)
        },
        inlineKeyboard {
            row {
                dataButton(
                    "✅ Закрыть, и пометить как выполненный",
                    "moderate_UniversityUpdateRequest_close_done_${updateRequest.id}"
                )
            }
            row {
                dataButton(
                    "❌ Закрыть без выполнения",
                    "moderate_UniversityUpdateRequest_close_${updateRequest.id}"
                )
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "moderate_UniversityUpdateRequest_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий➡\uFE0F", "moderate_UniversityUpdateRequest_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.notifyUserAboutUpdateRequestClosed(entity: UniversityUpdateRequestEntity) {
    send(
        ChatId(RawChatId(entity.botUser.userId)),
        buildEntities {
            +bold("Изменение статуса запроса на изменение информации #${entity.id.value} о ${entity.university.shortName}") +
                    "\n" + "- Статус изменен с ${UniversityUpdateRequestStatus.Open.text} на ${entity.status.text}"

            if (entity.moderator != null && entity.commentFromModeration != null) {
                +"\n" + "- Комментарий от модератора #${entity.moderator!!.id}:" + "\n" + blockquote(entity.commentFromModeration!!)
            }
        }
    )
}

suspend fun BehaviourContext.isUpdateRequestMayClosed(
    query: DataCallbackQuery,
    updateRequestId: Int
): Pair<Boolean, UniversityUpdateRequestEntity?> = databaseQuery {
    val updateRequestEntity = UniversityUpdateRequestEntity.findById(updateRequestId)

    if (updateRequestEntity == null) {
        answerCallbackQuery(
            query,
            "❌ Этого запроса не существует", showAlert = true
        )

        return@databaseQuery false to null
    }

    if (updateRequestEntity.status != UniversityUpdateRequestStatus.Open) {
        answerCallbackQuery(
            query,
            "❌ Этот запрос уже был закрыт", showAlert = true
        )

        return@databaseQuery false to updateRequestEntity
    }

    return@databaseQuery true to updateRequestEntity
}