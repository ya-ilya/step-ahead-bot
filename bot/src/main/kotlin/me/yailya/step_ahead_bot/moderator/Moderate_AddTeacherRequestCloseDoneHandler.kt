package me.yailya.step_ahead_bot.moderator

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.teacher.TeacherEntity
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequestStatus

suspend fun BehaviourContext.moderateHandleAddTeacherRequestCloseDoneCallback(
    query: DataCallbackQuery,
    addTeacherRequestId: Int
) {
    val (botUserEntity, _) = query.botUser()
    val (isCheckSuccessful, addTeacherRequestEntity) = checkAddTeacherRequestNotClosed(query, addTeacherRequestId)

    if (!isCheckSuccessful) {
        return
    }

    val teacherEntity = databaseQuery {
        addTeacherRequestEntity!!.apply {
            this.status = AddTeacherRequestStatus.ClosedAndDone
            this.moderator = botUserEntity
        }

        TeacherEntity.new {
            this.fullName = addTeacherRequestEntity.fullName
            this.university = addTeacherRequestEntity.university
            this.experience = addTeacherRequestEntity.experience
            this.academicTitle = addTeacherRequestEntity.academicTitle
            this.specialities = addTeacherRequestEntity.specialities
        }.also {
            notifyUserAboutAddTeacherRequestClosed(addTeacherRequestEntity)
        }
    }

    reply(
        to = query,
        "✅ Запрос на добавление нового преподавателя #${addTeacherRequestId} закрыт с пометкой о выполнении. ID добавленного преподавателя: ${teacherEntity.id}"
    )

    answerCallbackQuery(query)
}