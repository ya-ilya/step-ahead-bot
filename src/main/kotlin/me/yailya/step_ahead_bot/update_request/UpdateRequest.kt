package me.yailya.step_ahead_bot.update_request

import java.util.*

class UpdateRequest(
    val id: Int,
    val userId: Long,
    val universityId: Int,
    val text: String,
    val moderatorId: UUID?,
    val commentFromModeration: String?,
    val status: UpdateRequestStatus
)