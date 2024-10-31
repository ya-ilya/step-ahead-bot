@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.bot_user

import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.CallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import me.yailya.step_ahead_bot.databaseQuery

suspend fun CallbackQuery.botUser() = databaseQuery {
    message!!.botUser()
}

suspend fun AccessibleMessage.botUser() = databaseQuery {
    val userId = chat.id.chatId.long
    (BotUserEntity.find { BotUsers.userId eq userId }.singleOrNull() ?: BotUserEntity.new {
        this.userId = userId
    }).let { it to it.toModel() }
}