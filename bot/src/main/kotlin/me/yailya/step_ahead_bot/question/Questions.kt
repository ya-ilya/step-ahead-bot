package me.yailya.step_ahead_bot.question

import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.university.Universities
import org.jetbrains.exposed.dao.id.IntIdTable

object Questions : IntIdTable() {
    val university = reference("university", Universities)
    val botUser = reference("botUser", BotUsers)
    val text = text("text")
}