package me.yailya.step_ahead_bot.question

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.question.answer.AnswerEntity
import me.yailya.step_ahead_bot.question.answer.Answers
import me.yailya.step_ahead_bot.university.UniversityEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class QuestionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<QuestionEntity>(Questions)

    var university by UniversityEntity referencedOn Questions.university
    var botUser by BotUserEntity referencedOn Questions.botUser
    var text by Questions.text

    val answers by AnswerEntity referrersOn Answers.question

    fun toModel() = Question(
        id.value,
        university.toModel(),
        botUser.toModel(),
        text
    )
}