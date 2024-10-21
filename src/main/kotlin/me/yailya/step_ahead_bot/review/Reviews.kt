package me.yailya.step_ahead_bot.review

import org.jetbrains.exposed.dao.id.IntIdTable

object Reviews : IntIdTable() {
    val userId = long("userId")
    val universityId = integer("universityId")
    val pros = text("pros")
    val cons = text("cons")
    val comment = text("comment")
    val rating = integer("rating")
}