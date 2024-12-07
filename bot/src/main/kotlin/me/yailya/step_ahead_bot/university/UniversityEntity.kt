package me.yailya.step_ahead_bot.university

import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.olympiad.university_entry.OlympiadUniversityEntries
import me.yailya.step_ahead_bot.olympiad.university_entry.OlympiadUniversityEntryEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UniversityEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UniversityEntity>(Universities) {
        suspend fun getUniversityModels() = databaseQuery {
            all().map { it.toModel() }
        }
    }

    val shortName by Universities.shortName
    val name by Universities.name
    val nameEn by Universities.nameEn
    val description by Universities.description
    val website by Universities.website
    val location by Universities.location
    val facilities by Universities.facilities
    val budgetInfo by Universities.budgetInfo
    val paidInfo by Universities.paidInfo
    val extraPoints by Universities.extraPoints
    val inNumbers by Universities.inNumbers
    val contacts by Universities.contacts
    val socialNetworks by Universities.socialNetworks
    val listOfApplicants by Universities.listOfApplication
    val specialities by Universities.specialities

    val olympiads by OlympiadUniversityEntryEntity referrersOn OlympiadUniversityEntries.university

    fun toModel() = University(
        id.value,
        shortName,
        name,
        nameEn,
        description,
        website,
        location,
        facilities,
        budgetInfo,
        paidInfo,
        extraPoints,
        inNumbers,
        contacts,
        socialNetworks,
        listOfApplicants,
        specialities
    )
}