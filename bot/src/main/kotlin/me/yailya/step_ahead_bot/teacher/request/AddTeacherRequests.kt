package me.yailya.step_ahead_bot.teacher.request

import kotlinx.serialization.json.Json
import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.teacher.TeacherAcademicTitle
import me.yailya.step_ahead_bot.university.Universities
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json

object AddTeacherRequests : IntIdTable() {
    val botUser = reference("botUser", BotUsers)
    val university = reference("university", Universities)
    val fullName = text("fullName")
    val experience = integer("experience")
    val academicTitle = enumeration<TeacherAcademicTitle>("academicTitle")
    val specialities = json<List<String>>("specialities", Json)
    val moderator = reference("moderator", BotUsers).nullable()
    val status = enumeration<AddTeacherRequestStatus>("status")
}