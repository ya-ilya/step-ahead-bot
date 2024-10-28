@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequestStatus

suspend fun BehaviourContext.handleCreateUpdateRequestCallback(
    query: DataCallbackQuery,
    university: University
) {
    val textMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("Создание запроса на изменение информации о ${university.shortName}") +
                        "\n" + "Какую информацию, по вашему мнению, нужно изменить?"
            }
        )
    ).first()

    val updateRequest = databaseQuery {
        UpdateRequestEntity.new {
            this.userId = query.user.id.chatId.long
            this.universityId = university.id
            this.text = textMessage.content.text
            this.status = UpdateRequestStatus.Open
        }.toModel()
    }

    reply(
        to = textMessage,
        text = "Спасибо за ваше запрос на изменение информации о ${university.shortName}. В скором времени он будет рассмотрен модерацией. Номер запроса: #${updateRequest.id}"
    )
}