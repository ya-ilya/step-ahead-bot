package me.yailya.step_ahead_bot.question

import me.yailya.step_ahead_bot.bot_user.BotUser
import me.yailya.step_ahead_bot.university.University

class Question(
    val id: Int,
    val university: University,
    val botUser: BotUser,
    val text: String
)