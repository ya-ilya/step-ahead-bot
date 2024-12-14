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
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequest
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequestEntity
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequestStatus
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequests
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

suspend fun BehaviourContext.moderateHandleAddTeacherRequestCallback(
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
            +"${university.shortName} -> Запрос на добавление нового преподавателя #${addTeacherRequest.id}]" +
                    "\n- ФИО: ${addTeacherRequest.fullName}" +
                    "\n- Опыт работы: ${addTeacherRequest.experience}" +
                    "\n- Академическая должность: ${addTeacherRequest.academicTitle}" +
                    "\n- Специальности: ${addTeacherRequest.specialities.joinToString()}"
        },
        inlineKeyboard {
            row {
                dataButton(
                    "✅ Закрыть, и пометить как выполненный",
                    "moderate_AddTeacherRequest_close_done_${addTeacherRequest.id}"
                )
            }
            row {
                dataButton(
                    "❌ Закрыть без выполнения",
                    "moderate_AddTeacherRequest_close_${addTeacherRequest.id}"
                )
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "moderate_AddTeacherRequest_${previous.id}")
                }
                if (next != null) {
                    dataButton("Следующий➡\uFE0F", "moderate_AddTeacherRequest_${next.id}")
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

suspend fun BehaviourContext.isAddTeacherRequestMayClosed(
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