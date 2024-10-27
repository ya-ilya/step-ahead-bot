package me.yailya.step_ahead_bot.update_request

import org.jetbrains.exposed.dao.id.IntIdTable

object UpdateRequests : IntIdTable() {
    val userId = long("userId")
    val universityId = integer("universityId")
    val text = text("text")
    val moderatorId = uuid("moderatorId").nullable().default(null)
    val responseFromModeration = text("responseFromModeration").nullable().default("")
    val status = enumeration<UpdateRequestStatus>("status")
}