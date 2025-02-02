package me.yailya.step_ahead_bot.university.update_request.handlers

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
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequest
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestEntity
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestStatus
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequests
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun universityUpdateRequestForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<UniversityUpdateRequest?, UniversityUpdateRequest, UniversityUpdateRequest?> = databaseQuery {
    val botUserEntity = query.botUser
    val condition = UniversityUpdateRequests.botUser eq botUserEntity.id
    val updateRequests = botUserEntity.updateRequests

    if (updateRequests.empty()) {
        throw RuntimeException("❌ Вы еще не создавали запросы на изменение информации")
    }

    val current = if (id == -1) {
        updateRequests.first()
    } else {
        UniversityUpdateRequestEntity.findById(id)
            ?: throw RuntimeException("❌ Данный запрос на изменение информации не существует")
    }

    if (current.botUser.id != botUserEntity.id) {
        throw RuntimeException("❌ Данный запрос на изменение информации создали не вы")
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

suspend fun BehaviourContext.handleUniversityUpdateRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (previous, updateRequest, next) = try {
        universityUpdateRequestForKeyboard(query, updateRequestId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message, showAlert = true)
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
            if (updateRequest.status == UniversityUpdateRequestStatus.Open) {
                row {
                    dataButton("❌ Закрыть", "UniversityUpdateRequest_close_${updateRequest.id}")
                }
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "UniversityUpdateRequest_${previous.id}")

                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "UniversityUpdateRequest_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.isUpdateRequestMayClosed(
    query: DataCallbackQuery,
    updateRequestId: Int
): Pair<Boolean, UniversityUpdateRequestEntity?> = databaseQuery {
    val (botUserEntity) = query.botUser()
    val updateRequestEntity = UniversityUpdateRequestEntity.findById(updateRequestId)

    if (updateRequestEntity == null) {
        answerCallbackQuery(
            query,
            "❌ Этого запроса не существует",
            showAlert = true
        )

        return@databaseQuery false to null
    }

    if (updateRequestEntity.botUser != botUserEntity) {
        answerCallbackQuery(
            query,
            "❌ Вы не можете закрыть не ваш запрос на изменение информации",
            showAlert = true
        )
    }

    if (updateRequestEntity.status != UniversityUpdateRequestStatus.Open) {
        answerCallbackQuery(
            query,
            "❌ Этот запрос уже был закрыт",
            showAlert = true
        )

        return@databaseQuery false to updateRequestEntity
    }

    return@databaseQuery true to updateRequestEntity
}