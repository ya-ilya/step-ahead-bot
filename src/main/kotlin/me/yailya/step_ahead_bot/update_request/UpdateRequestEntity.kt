package me.yailya.step_ahead_bot.update_request

import me.yailya.step_ahead_bot.databaseQuery
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UpdateRequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UpdateRequestEntity>(UpdateRequests) {
        suspend fun getModelsByUserId(userId: Long) = databaseQuery {
            find { UpdateRequests.userId eq userId }.map { it.toModel() }
        }

        suspend fun getModelsByStatus(status: UpdateRequestStatus) = databaseQuery {
            find { UpdateRequests.status eq status }.map { it.toModel() }
        }
    }

    var userId by UpdateRequests.userId
    var universityId by UpdateRequests.universityId
    var text by UpdateRequests.text
    var moderatorId by UpdateRequests.moderatorId
    var responseFromModeration by UpdateRequests.responseFromModeration
    var status by UpdateRequests.status

    fun toModel() = UpdateRequest(
        id.value,
        userId,
        universityId,
        text,
        moderatorId,
        responseFromModeration,
        status
    )
}