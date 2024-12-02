package me.yailya.step_ahead_bot.moderator

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequestStatus

suspend fun BehaviourContext.moderateHandleAddTeacherRequestCloseCallback(
    query: DataCallbackQuery,
    addTeacherRequestId: Int
) {
    val (botUserEntity, _) = query.botUser()
    val (isCheckSuccessful, addTeacherRequestEntity) = checkAddTeacherRequestNotClosed(query, addTeacherRequestId)

    if (!isCheckSuccessful) {
        return
    }

    databaseQuery {
        addTeacherRequestEntity!!.apply {
            this.status = AddTeacherRequestStatus.Closed
            this.moderator = botUserEntity

            notifyUserAboutAddTeacherRequestClosed(this)
        }
    }

    reply(
        to = query,
        "Запрос на добавление нового преподавателя #${addTeacherRequestId} успешно закрыт без пометки о выполнении"
    )

    answerCallbackQuery(query)
}