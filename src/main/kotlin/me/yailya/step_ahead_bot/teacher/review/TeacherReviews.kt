package me.yailya.step_ahead_bot.teacher.review

import me.yailya.step_ahead_bot.teacher.Teachers
import org.jetbrains.exposed.dao.id.IntIdTable

object TeacherReviews : IntIdTable() {
    val teacher = reference("teacher", Teachers)
    val comment = text("comment")
    val rating = integer("rating")
}