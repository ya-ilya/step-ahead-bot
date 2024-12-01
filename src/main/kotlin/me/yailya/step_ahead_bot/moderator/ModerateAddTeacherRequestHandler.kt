package me.yailya.step_ahead_bot.moderator

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.teacher.TeacherEntity
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequest
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequestEntity
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequestStatus
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequests
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

private suspend fun addTeacherRequestForKeyboard(id: Int): Triple<AddTeacherRequest?, AddTeacherRequest, AddTeacherRequest?> =
    databaseQuery {
        val condition = AddTeacherRequests.status eq AddTeacherRequestStatus.Open
        val addTeacherRequests = AddTeacherRequestEntity.find(condition)

        if (addTeacherRequests.empty()) {
            throw RuntimeException("❌ Открытых запросов на добавление новых преподавателей не найдено")
        }

        val current = if (id == -1) {
            addTeacherRequests.first()
        } else {
            AddTeacherRequestEntity.findById(id)
                ?: throw RuntimeException("❌ Данный запрос на добавление преподавателя не существует")
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

suspend fun BehaviourContext.handleModerateAddTeacherRequestCallback(
    query: DataCallbackQuery,
    updateRequestId: Int
) {
    val (previous, addTeacherRequest, next) = try {
        addTeacherRequestForKeyboard(updateRequestId)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    val university = addTeacherRequest.university

    replyOrEdit(
        updateRequestId == -1,
        query,
        buildEntities {
            +"${university.shortName} -> Запрос на добавление нового преподавателя #${addTeacherRequest.id}]\n- Статус: ${addTeacherRequest.status.text}" +
                    "\n- ФИО: ${addTeacherRequest.fullName}" +
                    "\n- Опыт работы: ${addTeacherRequest.experience}" +
                    "\n- Академическая должность: ${addTeacherRequest.academicTitle}" +
                    "\n- Специальности: ${addTeacherRequest.specialities.joinToString()}"
        },
        inlineKeyboard {
            row {
                dataButton(
                    "✅ Закрыть, и пометить как выполненый",
                    "moderate_add_teacher_request_close_done_${addTeacherRequest.id}"
                )
            }
            row {
                dataButton("❌ Закрыть без выполнения", "moderate_add_teacher_request_close_${addTeacherRequest.id}")
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "moderate_add_teacher_request_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий➡\uFE0F", "moderate_add_teacher_request_${next.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.notifyUserAboutAddTeacherRequestClosed(
    entity: AddTeacherRequestEntity,
    teacherEntity: TeacherEntity? = null
) {
    send(
        ChatId(RawChatId(entity.botUser.userId)),
        buildEntities {
            +bold("Изменение статуса запроса на добавление нового преподавателя ${entity.university.shortName} #${entity.id.value}") +
                    "\n" + "- Статус изменен с ${AddTeacherRequestStatus.Open.text} на ${entity.status.text}"

            if (teacherEntity != null) {
                +"\n" + "- ID добавленного преподавателя: ${teacherEntity.id}"
            }
        }
    )
}

suspend fun BehaviourContext.checkAddTeacherRequestNotClosed(
    query: DataCallbackQuery,
    addTeacherRequestId: Int
): Pair<Boolean, AddTeacherRequestEntity?> {
    val addTeacherRequestEntity = databaseQuery { AddTeacherRequestEntity.findById(addTeacherRequestId) }

    if (addTeacherRequestEntity == null) {
        answerCallbackQuery(
            query,
            "❌ Этого запроса не существует"
        )

        return false to null
    }

    if (databaseQuery { addTeacherRequestEntity.status } != AddTeacherRequestStatus.Open) {
        answerCallbackQuery(
            query,
            "❌ Этот запрос уже был закрыт"
        )

        return false to addTeacherRequestEntity
    }

    return true to addTeacherRequestEntity
}