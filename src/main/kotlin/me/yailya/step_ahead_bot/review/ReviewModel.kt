package me.yailya.step_ahead_bot.review

data class ReviewModel(
    val id: Int,
    val userId: Long,
    val universityId: Int,
    val pros: String,
    val cons: String,
    val comment: String,
    val rating: Int
)