package me.yailya.step_ahead_bot.olympiad

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class OlympiadEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OlympiadEntity>(Olympiads)

    var name by Olympiads.name
    var level by Olympiads.level
    var website by Olympiads.website

    fun toModel() = Olympiad(
        id.value,
        name,
        level,
        website
    )
}