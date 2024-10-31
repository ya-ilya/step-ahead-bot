package me.yailya.step_ahead_bot.update_request

import me.yailya.step_ahead_bot.bot_user.BotUser

class UpdateRequest(
    val id: Int,
    val botUser: BotUser,
    val universityId: Int,
    val text: String,
    val moderator: BotUser?,
    val commentFromModeration: String?,
    val status: UpdateRequestStatus
)