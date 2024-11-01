package me.yailya.step_ahead_bot.question

import me.yailya.step_ahead_bot.bot_user.BotUsers
import org.jetbrains.exposed.dao.id.IntIdTable

object Questions : IntIdTable() {
    val universityId = integer("universityId")
    val botUser = reference("botUser", BotUsers)
    val text = text("text")
}