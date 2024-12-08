package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.LinkPreviewOptions
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

suspend fun olympiadUniversityEntriesForKeyboard(
    index: Int,
    university: University
): Triple<Int?, List<OlympiadUniversityEntry>, Int?> = databaseQuery {
    val condition = OlympiadUniversityEntries.university eq university.id
    val olympiads = OlympiadUniversityEntryEntity.find(condition)

    if (olympiads.empty()) {
        throw RuntimeException("❌ Олимпиад для поступления в ${university.shortName} не найдено")
    }

    if (index * 3 >= olympiads.count()) {
        throw RuntimeException("❌ Невозможно получить олимпиады по данному индексу")
    }

    val previous = if (olympiads.elementAtOrNull(index * 3 - 1) != null) {
        index - 1
    } else {
        null
    }
    val next = if (olympiads.elementAtOrNull(index * 3 + 3) != null) {
        index + 1
    } else {
        null
    }

    return@databaseQuery Triple(
        previous,
        arrayOf(index * 3, index * 3 + 1, index * 3 + 2)
            .mapNotNull { OlympiadUniversityEntryEntity.findById(it) }
            .map { it.toModel() },
        next
    )
}

suspend fun BehaviourContext.universityHandleOlympiadCallback(
    query: DataCallbackQuery,
    index: Int,
    university: University
) {
    val (previous, olympiadUniversityEntries, next) = try {
        olympiadUniversityEntriesForKeyboard(if (index == -1) 0 else index, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        index == -1,
        query,
        buildEntities {
            for (olympiadUniversityEntry in olympiadUniversityEntries) {
                +bold("${olympiadUniversityEntry.id}. Олимпиада ${olympiadUniversityEntry.olympiad.name} (${olympiadUniversityEntry.subject}) для поступления в ${university.shortName}") +
                        "\n" + "- \uD83C\uDF10 Веб-сайт олимпиады: " + link(olympiadUniversityEntry.olympiad.website) +
                        "\n" + "- Для ${olympiadUniversityEntry.grade} класса" +
                        "\n" + "- Необходимо стать как минимум ${if (olympiadUniversityEntry.onlyWinner) "победителем" else "призером"}" +
                        "\n" + "- Олимпиада дает следующее преимущество при поступлении: ${olympiadUniversityEntry.benefit}" + "\n"
            }
        },
        inlineKeyboard {
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущие (3)", "university_Olympiad_${previous}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующие (3) ➡\uFE0F", "university_Olympiad_${next}_${university.id}")
                }
            }
        },
        linkPreviewOptions = LinkPreviewOptions.Disabled
    )

    answerCallbackQuery(query)
}