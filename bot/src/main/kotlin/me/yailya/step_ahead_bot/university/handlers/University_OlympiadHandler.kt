package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.message.textsources.link
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.olympiad.university_entry.OlympiadUniversityEntries
import me.yailya.step_ahead_bot.olympiad.university_entry.OlympiadUniversityEntry
import me.yailya.step_ahead_bot.olympiad.university_entry.OlympiadUniversityEntryEntity
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.university.University
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun olympiadUniversityEntry(
    id: Int,
    university: University
): Triple<OlympiadUniversityEntry?, OlympiadUniversityEntry, OlympiadUniversityEntry?> = databaseQuery {
    val condition = OlympiadUniversityEntries.university eq university.id
    val questions = OlympiadUniversityEntryEntity.find(condition)

    if (questions.empty()) {
        throw RuntimeException("❌ Олимпиад для поступления в ${university.shortName} не найдено")
    }

    val current = if (id == -1) {
        questions.first()
    } else {
        OlympiadUniversityEntryEntity.findById(id) ?: throw RuntimeException("❌ Данной олимпиады для этого вуза не найдено")
    }

    val previous = OlympiadUniversityEntryEntity
        .find { condition and (OlympiadUniversityEntries.id less current.id) }
        .lastOrNull()
    val next = OlympiadUniversityEntryEntity
        .find { condition and (OlympiadUniversityEntries.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.universityHandleOlympiadCallback(
    query: DataCallbackQuery,
    olympiadId: Int,
    university: University
) {
    val (previous, olympiadUniversityEntry, next) = try {
        olympiadUniversityEntry(olympiadId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        olympiadId == -1,
        query,
        buildEntities {
            +bold("Олимпиада ${olympiadUniversityEntry.olympiad.name} (${olympiadUniversityEntry.subject}) для поступления в ${university.shortName}") +
                    "\n" + "- \uD83C\uDF10 Веб-сайт олимпиады: " + link(olympiadUniversityEntry.olympiad.website) +
                    "\n" + "- Для ${olympiadUniversityEntry.grade} класса" +
                    "\n" + "- Необходимо стать как минимум ${if (olympiadUniversityEntry.onlyWinner) "победителем" else "призером"}" +
                    "\n" + "- Олимпиада дает следующее преимущество при поступлении: ${olympiadUniversityEntry.benefit}"
        },
        inlineKeyboard {
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущая", "university_Olympiad_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующая ➡\uFE0F", "university_Olympiad_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}