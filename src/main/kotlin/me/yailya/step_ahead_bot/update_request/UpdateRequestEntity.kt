package me.yailya.step_ahead_bot.update_request

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.databaseQuery
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UpdateRequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UpdateRequestEntity>(UpdateRequests) {
        suspend fun getModelsByStatus(status: UpdateRequestStatus) = databaseQuery {
            find { UpdateRequests.status eq status }.map { it.toModel() }
        }
    }

    var botUser by BotUserEntity referencedOn UpdateRequests.botUser
    var universityId by UpdateRequests.universityId
    var text by UpdateRequests.text
    var moderator by BotUserEntity optionalReferencedOn UpdateRequests.moderator
    var commentFromModeration by UpdateRequests.commentFromModeration
    var status by UpdateRequests.status

    fun toModel() = UpdateRequest(
        id.value,
        botUser.toModel(),
        universityId,
        text,
        moderator?.toModel(),
        commentFromModeration,
        status
    )
}