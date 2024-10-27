package me.yailya.step_ahead_bot.university.handlers

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.getOrNull
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus
import me.yailya.step_ahead_bot.update_request.inputs.UpdateRequestConversationInputs

val updateRequestInputs = mutableMapOf<Long, UpdateRequestConversationInputs>()

suspend fun handleCreateUpdateRequestCallback(
    user: User,
    bot: TelegramBot,
    university: University
) {
    bot.inputListener[user] = "university_create_update_request_step1_${university.id}"

    val message = message {
        "" - bold { "Создание запроса на изменение информации о ${university.shortName}" } -
                "\n" - "Какую информацию, по вашему мнению, нужно изменить?"
    }.sendAsync(user, bot).getOrNull()

    updateRequestInputs[user.id] = UpdateRequestConversationInputs(originMessageId = message!!.messageId)
}

suspend fun handleCreateUpdateRequestStep1Input(
    user: User,
    bot: TelegramBot,
    university: University,
    input: String,
    messageId: Long
) {
    updateRequestInputs[user.id]!!.apply {
        inputs.add(input)
        messages.add(messageId)
    }

    val currentUserInputs = updateRequestInputs[user.id]!!

    for (message in currentUserInputs.messages) {
        deleteMessage(message).send(user, bot)
    }

    val updateRequest = databaseQuery {
        UpdateRequestEntity.new {
            this.userId = user.id
            this.universityId = university.id
            this.text = currentUserInputs.inputs[0]
            this.status = UpdateRequestStatus.Open
        }.toModel()
    }

    bot.inputListener.del(user.id)
    updateRequestInputs.remove(user.id)

    editMessageText(currentUserInputs.originMessageId) {
        "Спасибо за ваше запрос на изменение информации о ${university.shortName}. В скором времени он будет рассмотрен модерацией. Номер запроса: #${updateRequest.id}"
    }.send(user, bot)
}