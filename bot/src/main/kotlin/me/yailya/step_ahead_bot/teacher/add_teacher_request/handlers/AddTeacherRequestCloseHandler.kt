package me.yailya.step_ahead_bot.teacher.add_teacher_request.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequestEntity
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequestStatus

suspend fun BehaviourContext.handleAddTeacherRequestCloseCallback(
    query: DataCallbackQuery,
    addTeacherRequestId: Int
) {
    databaseQuery {
        AddTeacherRequestEntity.findById(addTeacherRequestId)!!.apply {
            this.status = AddTeacherRequestStatus.Closed
        }
    }

    editInlineButton(
        query,
        { button -> button.text.contains("Закрыть") },
        null
    )

    reply(
        to = query,
        text = "✅ Запрос на добавление нового преподавателя #${addTeacherRequestId} был успешно закрыт"
    )

    answerCallbackQuery(query)
}