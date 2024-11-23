package me.yailya.step_ahead_bot.bot_user

import java.time.LocalDateTime

class BotUser(
    val id: Int,
    val userId: Long,
    val isModerator: Boolean,
    val isAdministrator: Boolean,
    val lastQuestionTime: LocalDateTime?,
    val lastQuestionAnswerTime: LocalDateTime?,
    val lastTeacherReviewTime: LocalDateTime?,
    val lastReviewTime: LocalDateTime?,
    val lastUpdateRequestTime: LocalDateTime?,
    val lastAddTeacherRequestTime: LocalDateTime?
)