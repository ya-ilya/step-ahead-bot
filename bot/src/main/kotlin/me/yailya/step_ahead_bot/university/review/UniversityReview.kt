package me.yailya.step_ahead_bot.university.review

import me.yailya.step_ahead_bot.bot_user.BotUser
import me.yailya.step_ahead_bot.university.University

data class UniversityReview(
    val id: Int,
    val botUser: BotUser,
    val university: University,
    val pros: String,
    val cons: String,
    val comment: String,
    val rating: Int
)