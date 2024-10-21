package me.yailya.step_ahead_bot.review

import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.university.University
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ReviewEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ReviewEntity>(Reviews) {
        suspend fun queriedModelsByUniversity(university: University) = databaseQuery {
            find { Reviews.universityId eq university.id }.map { it.toModel() }
        }

        suspend fun queriedAllModels() = databaseQuery { all().map { it.toModel() } }
    }

    var userId by Reviews.userId
    var universityId by Reviews.universityId
    var pros by Reviews.pros
    var cons by Reviews.cons
    var comment by Reviews.comment
    var rating by Reviews.rating

    suspend fun queriedToModel() = databaseQuery { toModel() }

    fun toModel() = ReviewModel(
        id.value,
        userId,
        universityId,
        pros,
        cons,
        comment,
        rating
    )
}