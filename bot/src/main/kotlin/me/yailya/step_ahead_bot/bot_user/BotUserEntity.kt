package me.yailya.step_ahead_bot.bot_user

import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.question.answer.QuestionAnswerEntity
import me.yailya.step_ahead_bot.question.answer.QuestionAnswers
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequestEntity
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequests
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import me.yailya.step_ahead_bot.university.review.UniversityReviewEntity
import me.yailya.step_ahead_bot.university.review.UniversityReviews
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequestEntity
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequests
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BotUserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BotUserEntity>(BotUsers)

    var userId by BotUsers.userId
    var isModerator by BotUsers.isModerator
    var isAdministrator by BotUsers.isAdministrator
    var lastQuestionTime by BotUsers.lastQuestionTime
    var lastQuestionAnswerTime by BotUsers.lastQuestionAnswerTime
    var lastTeacherReviewTime by BotUsers.lastTeacherReviewTime
    var lastReviewTime by BotUsers.lastReviewTime
    var lastUpdateRequestTime by BotUsers.lastUpdateRequestTime
    var lastAddTeacherRequestTime by BotUsers.lastAddTeacherRequestTime

    val questions by QuestionEntity referrersOn Questions.botUser
    val answers by QuestionAnswerEntity referrersOn QuestionAnswers.botUser
    val reviews by UniversityReviewEntity referrersOn UniversityReviews.botUser
    val updateRequests by UniversityUpdateRequestEntity referrersOn UniversityUpdateRequests.botUser
    val teacherReviews by TeacherReviewEntity referrersOn TeacherReviews.botUser
    val addTeacherRequests by AddTeacherRequestEntity referrersOn AddTeacherRequests.botUser

    fun toModel() = BotUser(
        id.value,
        userId,
        isModerator,
        isAdministrator,
        lastQuestionTime,
        lastQuestionAnswerTime,
        lastTeacherReviewTime,
        lastReviewTime,
        lastUpdateRequestTime,
        lastAddTeacherRequestTime
    )
}