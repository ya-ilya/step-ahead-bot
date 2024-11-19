package me.yailya.step_ahead_bot.teacher

import me.yailya.step_ahead_bot.university.University

class Teacher(
    val id: Int,
    val fullName: String,
    val experience: Int,
    val academicTitle: TeacherAcademicTitle,
    val university: University,
    val specialities: List<String>
)