package me.yailya.step_ahead_bot.teacher.review

import me.yailya.step_ahead_bot.bot_user.BotUser
import me.yailya.step_ahead_bot.teacher.Teacher

class TeacherReview(
    val id: Int,
    val botUser: BotUser,
    val teacher: Teacher,
    val comment: String,
    val rating: Int
)