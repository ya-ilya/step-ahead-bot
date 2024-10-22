package me.yailya.step_ahead_bot.review

data class Review(
    val id: Int,
    val userId: Long,
    val universityId: Int,
    val pros: String,
    val cons: String,
    val comment: String,
    val rating: Int
)