package me.yailya.step_ahead_bot.question.answer

import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.question.Questions
import org.jetbrains.exposed.dao.id.IntIdTable

object Answers : IntIdTable() {
    val botUser = reference("botUser", BotUsers)
    val question = reference("question", Questions)
    val text = text("text")
    val isAccepted = bool("accepted").default(false)
}