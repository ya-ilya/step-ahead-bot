package me.yailya.step_ahead_bot.question.answer

import me.yailya.step_ahead_bot.bot_user.BotUser
import me.yailya.step_ahead_bot.question.Question

class Answer(
    val id: Int,
    val botUser: BotUser,
    val question: Question,
    val text: String,
    val isAccepted: Boolean
)