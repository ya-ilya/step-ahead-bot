package me.yailya.step_ahead_bot.university

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json

object Universities : IntIdTable() {
    val shortName = varchar("shortName", 1024)
    val name = varchar("name", 1024)
    val nameEn = varchar("nameEn", 1024)
    val description = varchar("description", 8192)
    val website = varchar("website", 1024)
    val location = varchar("location", 1024)
    val facilities = json<List<University.UniversityFacility>>("facilities", Json)
    val budgetInfo = json<University.UniversityBudgetInfo>("budgetInfo", Json)
    val paidInfo = json<University.UniversityPaidInfo>("paidInfo", Json)
    val extraPoints = json<Map<University.ExtraPointsReason, Int>>("extraPoints", Json)
    val inNumbers = json<University.UniversityInNumbers>("inNumbers", Json)
    val contacts = json<University.UniversityContacts>("contacts", Json)
    val socialNetworks = json<University.UniversitySocialNetworks>("socialNetworks", Json)
    val listOfApplication = varchar("listOfApplicants", 1024)
    val specialities = json<List<String>>("specialities", Json)
}