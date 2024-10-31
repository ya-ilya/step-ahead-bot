package me.yailya.step_ahead_bot.bot_user

import org.jetbrains.exposed.dao.id.IntIdTable

object BotUsers : IntIdTable() {
    val userId = long("userId")
    val isModerator = bool("isModerator")
}