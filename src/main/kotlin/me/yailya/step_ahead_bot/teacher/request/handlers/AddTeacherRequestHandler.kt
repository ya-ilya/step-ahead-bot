package me.yailya.step_ahead_bot.teacher.request.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequest
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequestEntity
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequestStatus
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequests
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun addTeacherRequestForKeyboard(
    query: DataCallbackQuery,
    id: Int
): Triple<AddTeacherRequest?, AddTeacherRequest, AddTeacherRequest?> = databaseQuery {
    val botUserEntity = query.botUser
    val condition = AddTeacherRequests.botUser eq botUserEntity.id
    val addTeacherRequests = botUserEntity.addTeacherRequests

    if (addTeacherRequests.empty()) {
        throw RuntimeException("❌ Вы еще не создавали запросы на добавление новых преподавателей")
    }

    val current = if (id == -1) {
        addTeacherRequests.first()
    } else {
        AddTeacherRequestEntity.findById(id)
            ?: throw RuntimeException("❌ Данный запрос на добавление нового преподавателя не существует")
    }

    if (current.botUser.id != botUserEntity.id) {
        throw RuntimeException("❌ Данный запрос на добавление нового преподавателя создали не вы")
    }

    val previous = AddTeacherRequestEntity
        .find { condition and (AddTeacherRequests.id less current.id) }
        .lastOrNull()
    val next = AddTeacherRequestEntity
        .find { condition and (AddTeacherRequests.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleAddTeacherRequestCallback(
    query: DataCallbackQuery,
    addTeacherRequestId: Int
) {
    val (previous, addTeacherRequest, next) = try {
        addTeacherRequestForKeyboard(query, addTeacherRequestId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    val university = addTeacherRequest.university

    replyOrEdit(
        addTeacherRequestId == -1,
        query,
        buildEntities {
            +"\n" + "${university.shortName} -> Запрос на добавление нового преподавателя #${addTeacherRequest.id}\n- Статус: ${addTeacherRequest.status.text}" +
                    "\n- ФИО: ${addTeacherRequest.fullName}" +
                    "\n- Опыт работы: ${addTeacherRequest.experience}" +
                    "\n- Академическая должность: ${addTeacherRequest.academicTitle}" +
                    "\n- Специальности: ${addTeacherRequest.specialities.joinToString()}"
        },
        inlineKeyboard {
            if (addTeacherRequest.status == AddTeacherRequestStatus.Open) {
                row {
                    dataButton("❌ Закрыть", "add_teacher_request_close_${addTeacherRequest.id}")
                }
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "add_teacher_request_${previous.id}")

                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "add_teacher_request_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleAddTeacherRequestCloseCallback(
    query: DataCallbackQuery,
    addTeacherRequestId: Int
) {
    databaseQuery {
        AddTeacherRequestEntity.findById(addTeacherRequestId)!!.apply {
            this.status = AddTeacherRequestStatus.Closed
        }
    }

    reply(
        to = query,
        text = "Запрос на добавление нового преподавателя #${addTeacherRequestId} был успешно закрыт"
    )

    answerCallbackQuery(query)
}