@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.moderator

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestStatus

suspend fun BehaviourContext.moderateHandleUniversityUpdateRequestCloseCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (botUserEntity, _) = query.botUser()
    val (isCheckSuccessful, updateRequestEntity) = isUpdateRequestMayClosed(query, updateRequestId)

    if (!isCheckSuccessful) {
        return
    }

    val university = databaseQuery {
        updateRequestEntity!!.university.toModel()
    }

    val commentMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("${university.shortName} -> Закрытие запроса на изменение информации #${updateRequestId} без пометки о выполнении") +
                        "\n" + "Прокомментируйте закрытие запроса:"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
        )
    ).first()

    databaseQuery {
        updateRequestEntity!!.apply {
            this.status = UniversityUpdateRequestStatus.Closed
            this.moderator = botUserEntity
            this.commentFromModeration = commentMessage.content.text

            notifyUserAboutUpdateRequestClosed(this)
        }
    }

    editInlineButton(
        query,
        { button -> button.text.contains("Закрыть") },
        null
    )

    reply(
        to = commentMessage,
        "❌ Запрос на изменение информации #${updateRequestId} успешно закрыт без пометки о выполнении"
    )

    answerCallbackQuery(query)
}