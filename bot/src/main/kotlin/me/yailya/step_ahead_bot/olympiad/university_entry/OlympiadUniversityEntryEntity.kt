package me.yailya.step_ahead_bot.olympiad.university_entry

import me.yailya.step_ahead_bot.olympiad.OlympiadEntity
import me.yailya.step_ahead_bot.university.UniversityEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class OlympiadUniversityEntryEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OlympiadUniversityEntryEntity>(OlympiadUniversityEntries)

    var olympiad by OlympiadEntity referencedOn OlympiadUniversityEntries.olympiad
    var university by UniversityEntity referencedOn OlympiadUniversityEntries.university
    var onlyWinner by OlympiadUniversityEntries.onlyWinner
    var benefit by OlympiadUniversityEntries.benefit

    fun toModel() = OlympiadUniversityEntry(
        id.value,
        olympiad.toModel(),
        university.toModel(),
        onlyWinner,
        benefit
    )
}