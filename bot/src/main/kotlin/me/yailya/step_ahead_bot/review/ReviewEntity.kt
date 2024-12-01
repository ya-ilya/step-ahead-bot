package me.yailya.step_ahead_bot.review

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.university.UniversityEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ReviewEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ReviewEntity>(Reviews)

    var botUser by BotUserEntity referencedOn Reviews.botUser
    var university by UniversityEntity referencedOn Reviews.university
    var pros by Reviews.pros
    var cons by Reviews.cons
    var comment by Reviews.comment
    var rating by Reviews.rating

    fun toModel() = Review(
        id.value,
        botUser.toModel(),
        university.toModel(),
        pros,
        cons,
        comment,
        rating
    )
}