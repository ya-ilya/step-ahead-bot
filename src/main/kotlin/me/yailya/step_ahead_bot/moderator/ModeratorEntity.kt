package me.yailya.step_ahead_bot.moderator

import me.yailya.step_ahead_bot.databaseQuery
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class ModeratorEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ModeratorEntity>(Moderators) {
        suspend fun getModels() = databaseQuery {
            ModeratorEntity.all().map { it.toModel() }
        }

        suspend fun getModeratorByUserId(userId: Long) = databaseQuery {
            ModeratorEntity.find { Moderators.userId eq userId }.singleOrNull()
        }
    }

    var userId by Moderators.userId

    fun toModel() = Moderator(
        id.value,
        userId
    )
}