package me.yailya.step_ahead_bot.university.update_request

import me.yailya.step_ahead_bot.bot_user.BotUser
import me.yailya.step_ahead_bot.university.University

class UniversityUpdateRequest(
    val id: Int,
    val botUser: BotUser,
    val university: University,
    val text: String,
    val moderator: BotUser?,
    val commentFromModeration: String?,
    val status: UniversityUpdateRequestStatus
)