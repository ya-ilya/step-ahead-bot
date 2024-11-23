package me.yailya.step_ahead_bot.bot_user

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object BotUsers : IntIdTable() {
    val userId = long("userId")
    val isModerator = bool("isModerator")
    val isAdministrator = bool("isAdministrator")
    val lastQuestionTime = datetime("lastQuestionTime").nullable()
    val lastQuestionAnswerTime = datetime("lastQuestionAnswerTime").nullable()
    val lastTeacherReviewTime = datetime("lastTeacherReviewTime").nullable()
    val lastReviewTime = datetime("lastReviewTime").nullable()
    val lastUpdateRequestTime = datetime("lastUpdateRequestTime").nullable()
    val lastAddTeacherRequestTime = datetime("lastAddTeacherRequestTime").nullable()
}