package me.yailya.step_ahead_bot.review

import me.yailya.step_ahead_bot.bot_user.BotUsers
import org.jetbrains.exposed.dao.id.IntIdTable

object Reviews : IntIdTable() {
    val botUser = reference("botUser", BotUsers)
    val universityId = integer("universityId")
    val pros = text("pros")
    val cons = text("cons")
    val comment = text("comment")
    val rating = integer("rating")
}