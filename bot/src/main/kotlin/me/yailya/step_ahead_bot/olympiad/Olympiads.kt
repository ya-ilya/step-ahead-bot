package me.yailya.step_ahead_bot.olympiad

import org.jetbrains.exposed.dao.id.IntIdTable

object Olympiads : IntIdTable() {
    val name = text("name")
    val website = text("website")
}