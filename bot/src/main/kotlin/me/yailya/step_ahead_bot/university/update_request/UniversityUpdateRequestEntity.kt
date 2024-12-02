package me.yailya.step_ahead_bot.university.update_request

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.university.UniversityEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UniversityUpdateRequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UniversityUpdateRequestEntity>(UniversityUpdateRequests)

    var botUser by BotUserEntity referencedOn UniversityUpdateRequests.botUser
    var university by UniversityEntity referencedOn UniversityUpdateRequests.university
    var text by UniversityUpdateRequests.text
    var moderator by BotUserEntity optionalReferencedOn UniversityUpdateRequests.moderator
    var commentFromModeration by UniversityUpdateRequests.commentFromModeration
    var status by UniversityUpdateRequests.status

    fun toModel() = UniversityUpdateRequest(
        id.value,
        botUser.toModel(),
        university.toModel(),
        text,
        moderator?.toModel(),
        commentFromModeration,
        status
    )
}