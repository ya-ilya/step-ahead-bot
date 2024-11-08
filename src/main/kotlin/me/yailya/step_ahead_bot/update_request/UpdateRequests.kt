package me.yailya.step_ahead_bot.update_request

import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.university.Universities
import org.jetbrains.exposed.dao.id.IntIdTable

object UpdateRequests : IntIdTable() {
    val botUser = reference("botUser", BotUsers)
    val university = reference("university", Universities)
    val text = text("text")
    val moderator = reference("moderator", BotUsers).nullable()
    val commentFromModeration = text("commentFromModeration").nullable()
    val status = enumeration<UpdateRequestStatus>("status")
}