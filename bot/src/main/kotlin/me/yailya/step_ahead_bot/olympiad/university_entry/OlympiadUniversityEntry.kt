package me.yailya.step_ahead_bot.olympiad.university_entry

import me.yailya.step_ahead_bot.olympiad.Olympiad
import me.yailya.step_ahead_bot.university.University

class OlympiadUniversityEntry(
    val id: Int,
    val olympiad: Olympiad,
    val university: University,
    val onlyWinner: Boolean,
    val benefit: String
)