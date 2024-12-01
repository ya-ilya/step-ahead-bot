package me.yailya.step_ahead_bot.teacher

import kotlinx.serialization.json.Json
import me.yailya.step_ahead_bot.university.Universities
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json

object Teachers : IntIdTable() {
    val fullName = text("fullName")
    val experience = integer("experience")
    val academicTitle = enumeration<TeacherAcademicTitle>("academicTitle")
    val university = reference("university", Universities)
    val specialities = json<List<String>>("specialities", Json)
}