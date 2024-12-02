package me.yailya.step_ahead_bot.university.review

import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.university.Universities
import org.jetbrains.exposed.dao.id.IntIdTable

object UniversityReviews : IntIdTable() {
    val botUser = reference("botUser", BotUsers)
    val university = reference("university", Universities)
    val pros = text("pros")
    val cons = text("cons")
    val comment = text("comment")
    val rating = integer("rating")
}