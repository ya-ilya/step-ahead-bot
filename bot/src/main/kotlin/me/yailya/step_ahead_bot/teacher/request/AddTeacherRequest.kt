package me.yailya.step_ahead_bot.teacher.request

import me.yailya.step_ahead_bot.bot_user.BotUser
import me.yailya.step_ahead_bot.teacher.TeacherAcademicTitle
import me.yailya.step_ahead_bot.university.University

class AddTeacherRequest(
    val id: Int,
    val botUser: BotUser,
    val university: University,
    val fullName: String,
    val experience: Int,
    val academicTitle: TeacherAcademicTitle,
    val specialities: List<String>,
    val moderator: BotUser?,
    val status: AddTeacherRequestStatus
)