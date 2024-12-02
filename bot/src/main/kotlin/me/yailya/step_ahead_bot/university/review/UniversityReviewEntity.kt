package me.yailya.step_ahead_bot.university.review

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.university.UniversityEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UniversityReviewEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UniversityReviewEntity>(UniversityReviews)

    var botUser by BotUserEntity referencedOn UniversityReviews.botUser
    var university by UniversityEntity referencedOn UniversityReviews.university
    var pros by UniversityReviews.pros
    var cons by UniversityReviews.cons
    var comment by UniversityReviews.comment
    var rating by UniversityReviews.rating

    fun toModel() = UniversityReview(
        id.value,
        botUser.toModel(),
        university.toModel(),
        pros,
        cons,
        comment,
        rating
    )
}