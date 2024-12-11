package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.teacher.Teacher
import me.yailya.step_ahead_bot.teacher.TeacherEntity
import me.yailya.step_ahead_bot.teacher.Teachers
import me.yailya.step_ahead_bot.university.University
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun teacherForKeyboard(
    id: Int,
    university: University
): Triple<Teacher?, Teacher, Teacher?> = databaseQuery {
    val condition = Teachers.university eq university.id
    val teachers = TeacherEntity.find(condition)

    if (teachers.empty()) {
        throw RuntimeException("❌ Преподавателей в ${university.shortName} пока не добавлено")
    }

    val current = if (id == -1) {
        teachers.first()
    } else {
        TeacherEntity.findById(id) ?: throw RuntimeException("❌ Данный преподаватель не найден")
    }

    val previous = TeacherEntity
        .find { condition and (Teachers.id less current.id) }
        .lastOrNull()
    val next = TeacherEntity
        .find { condition and (Teachers.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.universityHandleTeacherCallback(
    query: DataCallbackQuery,
    teacherId: Int,
    university: University
) {
    val (previous, teacher, next) = try {
        teacherForKeyboard(teacherId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        teacherId == -1,
        query,
        buildEntities {
            +bold("${university.shortName} -> Преподаватель ${teacher.fullName}") +
                    "\n- Опыт работы: ${teacher.experience}" +
                    "\n- Академическая должность: ${teacher.academicTitle}" +
                    "\n- Специальности: ${teacher.specialities.joinToString()}"
        },
        inlineKeyboard {
            row {
                dataButton(
                    "⭐ Отзывы о преподавателе",
                    "university_teacher_reviews_${teacher.id}_${university.id}"
                )
            }

            row {
                dataButton(
                    "✍\uD83C\uDFFB Оставить отзыв о преподавателе",
                    "university_create_teacher_review_${teacher.id}_${university.id}"
                )
            }

            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_Teacher_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_Teacher_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}