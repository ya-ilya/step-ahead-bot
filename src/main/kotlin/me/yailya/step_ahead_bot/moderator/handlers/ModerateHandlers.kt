package me.yailya.step_ahead_bot.moderator.handlers

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.update_request.UpdateRequest
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun sendUpdateRequestMessage(
    user: User,
    bot: TelegramBot,
    updateRequest: UpdateRequest,
    previousUpdateRequestId: Int,
    nextUpdateRequestId: Int
) {
    val university = Universities[updateRequest.universityId]

    message {
        "[Запрос №${updateRequest.id}]\n- Университет: ${university.name}\n- Статус: ${updateRequest.status.text}" -
                "\nИнформация, которую пользователь бы хотел поменять: " - blockquote { updateRequest.text }
    }.inlineKeyboardMarkup {
        "Закрыть запрос, и пометить как выполненое" callback "moderate_update_request_close_done_${updateRequest.id}"
        newLine()
        "Закрыть запрос без его выполнения" callback "moderate_update_request_close_${updateRequest.id}"
        if (previousUpdateRequestId != -1) {
            newLine()
            "Предыдущий" callback "moderate_update_request_${previousUpdateRequestId}"
        }
        if (nextUpdateRequestId != -1) {
            newLine()
            "Следущий" callback "moderate_update_request_${nextUpdateRequestId}"
        }
    }.send(user, bot)
}

suspend fun handleModerateUpdateRequestCallback(
    user: User,
    bot: TelegramBot,
    updateRequestId: Int,
    previousMessageId: Long? = null
) {
    if (previousMessageId != null) {
        deleteMessage(previousMessageId).send(user, bot)
    }

    val updateRequests = UpdateRequestEntity.getModelsByStatus(UpdateRequestStatus.Open)

    if (updateRequests.isEmpty()) {
        message {
            "" - bold { "Открытых запросов на изменение не найдено" }
        }.send(user, bot)

        return
    }

    val realUpdateRequestId = if (updateRequestId == -1) updateRequests.first().id else updateRequestId
    val currentElement = updateRequests.find { it.id == realUpdateRequestId }!!
    val currentElementId = updateRequests.indexOf(currentElement)
    val previousElement = updateRequests.elementAtOrNull(currentElementId - 1)
    val nextElement = updateRequests.elementAtOrNull(currentElementId + 1)

    sendUpdateRequestMessage(
        user,
        bot,
        currentElement,
        previousElement?.id ?: -1,
        nextElement?.id ?: -1
    )
}

suspend fun handleModerateUpdateRequestCloseDoneCallback(
    user: User,
    bot: TelegramBot,
    updateRequestId: Int
) {

}

suspend fun handleModerateUpdateRequestCloseCallback(
    user: User,
    bot: TelegramBot,
    updateRequestId: Int
) {

}