package me.yailya.step_ahead_bot.moderator

import org.jetbrains.exposed.dao.id.UUIDTable

object Moderators : UUIDTable() {
    val userId = long("userId")
}