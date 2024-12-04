package me.yailya.step_ahead_bot.olympiad.university_entry

import me.yailya.step_ahead_bot.olympiad.Olympiads
import me.yailya.step_ahead_bot.university.Universities
import org.jetbrains.exposed.dao.id.IntIdTable

object OlympiadUniversityEntries : IntIdTable() {
    val olympiad = reference("olympiad", Olympiads)
    val university = reference("university", Universities)
    val onlyWinner = bool("onlyWinner")
    val benefit = text("benefit")
}