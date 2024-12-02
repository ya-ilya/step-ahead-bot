package me.yailya.step_ahead_bot.teacher.add_teacher_request

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.university.UniversityEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class AddTeacherRequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AddTeacherRequestEntity>(AddTeacherRequests)

    var botUser by BotUserEntity referencedOn AddTeacherRequests.botUser
    var university by UniversityEntity referencedOn AddTeacherRequests.university
    var fullName by AddTeacherRequests.fullName
    var experience by AddTeacherRequests.experience
    var academicTitle by AddTeacherRequests.academicTitle
    var specialities by AddTeacherRequests.specialities
    var moderator by BotUserEntity optionalReferencedOn AddTeacherRequests.moderator
    var status by AddTeacherRequests.status

    fun toModel() = AddTeacherRequest(
        id.value,
        botUser.toModel(),
        university.toModel(),
        fullName,
        experience,
        academicTitle,
        specialities,
        moderator?.toModel(),
        status
    )
}