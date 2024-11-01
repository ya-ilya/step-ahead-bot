package me.yailya.step_ahead_bot.question

import me.yailya.step_ahead_bot.bot_user.BotUser

class Question(
    val id: Int,
    val universityId: Int,
    val botUser: BotUser,
    val text: String
)