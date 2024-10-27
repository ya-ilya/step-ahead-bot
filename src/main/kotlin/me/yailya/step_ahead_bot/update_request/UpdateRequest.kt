package me.yailya.step_ahead_bot.update_request

class UpdateRequest(
    val id: Int,
    val userId: Long,
    val universityId: Int,
    val text: String,
    val responseFromModeration: String,
    val status: UpdateRequestStatus
)