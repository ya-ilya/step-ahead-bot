package me.yailya.step_ahead_bot.teacher

import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import me.yailya.step_ahead_bot.university.UniversityEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class TeacherEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TeacherEntity>(Teachers)

    var fullName by Teachers.fullName
    var experience by Teachers.experience
    var academicTitle by Teachers.academicTitle
    var university by UniversityEntity referencedOn Teachers.university
    var specialities by Teachers.specialities

    val reviews by TeacherReviewEntity referrersOn TeacherReviews.teacher

    fun toModel() = Teacher(
        id.value,
        fullName,
        experience,
        academicTitle,
        university.toModel(),
        specialities,
        Math.round(reviews.map { it.rating }.average() * 100.0) / 100.0
    )
}