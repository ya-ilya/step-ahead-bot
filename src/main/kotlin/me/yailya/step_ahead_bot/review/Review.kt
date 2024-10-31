package me.yailya.step_ahead_bot.review

import me.yailya.step_ahead_bot.bot_user.BotUser

data class Review(
    val id: Int,
    val botUserId: BotUser,
    val universityId: Int,
    val pros: String,
    val cons: String,
    val comment: String,
    val rating: Int
)