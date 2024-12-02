@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
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
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.UniversityEntity
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestEntity
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestStatus
import java.time.LocalDateTime

suspend fun BehaviourContext.universityHandleCreateUniversityUpdateRequestCallback(
    query: DataCallbackQuery,
    university: University
) {
    val (botUserEntity, botUser) = query.botUser()

    if (botUser.lastUpdateRequestTime != null && LocalDateTime.now() < botUser.lastUpdateRequestTime.plusMinutes(1)) {
        answerCallbackQuery(
            query,
            "⏳ Вы должны подождать минуту, прежде чем создать новый запрос на изменение информации"
        )

        return
    }

    val textMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("${university.shortName} -> Создание запроса на изменение информации") +
                        "\n" + "Какую информацию, по вашему мнению, нужно изменить?"
            }
        )
    ).first()

    val updateRequest = databaseQuery {
        botUserEntity.lastUpdateRequestTime = LocalDateTime.now()

        UniversityUpdateRequestEntity.new {
            this.botUser = botUserEntity
            this.university = UniversityEntity.findById(university.id)!!
            this.text = textMessage.content.text
            this.status = UniversityUpdateRequestStatus.Open
        }.toModel()
    }

    reply(
        to = textMessage,
        text = "✅ Спасибо за ваше запрос на изменение информации о ${university.shortName}. В скором времени он будет рассмотрен модерацией. Номер запроса: #${updateRequest.id}"
    )

    answerCallbackQuery(query)
}