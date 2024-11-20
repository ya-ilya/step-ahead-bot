package me.yailya.step_ahead_bot.teacher.review

import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.teacher.Teachers
import org.jetbrains.exposed.dao.id.IntIdTable

object TeacherReviews : IntIdTable() {
    val botUser = reference("botUser", BotUsers)
    val teacher = reference("teacher", Teachers)
    val comment = text("comment")
    val rating = integer("rating")
}