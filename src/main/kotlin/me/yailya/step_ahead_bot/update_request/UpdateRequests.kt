package me.yailya.step_ahead_bot.update_request

import me.yailya.step_ahead_bot.bot_user.BotUsers
import org.jetbrains.exposed.dao.id.IntIdTable

object UpdateRequests : IntIdTable() {
    val botUser = reference("botUser", BotUsers)
    val universityId = integer("universityId")
    val text = text("text")
    val moderator = reference("moderator", BotUsers).nullable()
    val commentFromModeration = text("commentFromModeration").nullable().default("")
    val status = enumeration<UpdateRequestStatus>("status")
}