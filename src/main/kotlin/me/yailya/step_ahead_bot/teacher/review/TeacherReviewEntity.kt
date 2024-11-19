package me.yailya.step_ahead_bot.teacher.review

import me.yailya.step_ahead_bot.teacher.TeacherEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class TeacherReviewEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TeacherReviewEntity>(TeacherReviews)

    var teacher by TeacherEntity referencedOn TeacherReviews.teacher
    var comment by TeacherReviews.comment
    var rating by TeacherReviews.rating

    fun toModel() = TeacherReview(
        id.value,
        teacher.toModel(),
        comment,
        rating
    )
}