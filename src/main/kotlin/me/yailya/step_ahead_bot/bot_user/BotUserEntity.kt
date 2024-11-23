package me.yailya.step_ahead_bot.bot_user

import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.answer.Answers
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.review.Reviews
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequestEntity
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequests
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequests
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
    val answers by AnswerEntity referrersOn Answers.botUser
    val reviews by ReviewEntity referrersOn Reviews.botUser
    val updateRequests by UpdateRequestEntity referrersOn UpdateRequests.botUser
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