package me.yailya.step_ahead_bot.question

import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.answer.Answers
import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.university.University
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class QuestionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<QuestionEntity>(Questions) {
        suspend fun getModelsByUniversity(university: University) = databaseQuery {
            find { Questions.universityId eq university.id }.map { it.toModel() }
        }
    }

    var universityId by Questions.universityId
    var botUser by BotUserEntity referencedOn Questions.botUser
    var text by Questions.text

    val answers by AnswerEntity referrersOn Answers.question

    fun toModel() = Question(
        id.value,
        universityId,
        botUser.toModel(),
        text
    )
}