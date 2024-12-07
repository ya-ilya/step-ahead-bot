package me.yailya.step_ahead_bot.question.answer

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.question.QuestionEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class QuestionAnswerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<QuestionAnswerEntity>(QuestionAnswers)

    var botUser by BotUserEntity referencedOn QuestionAnswers.botUser
    var question by QuestionEntity referencedOn QuestionAnswers.question
    var text by QuestionAnswers.text
    var isAccepted by QuestionAnswers.isAccepted

    fun toModel() = QuestionAnswer(
        id.value,
        botUser.toModel(),
        question.toModel(),
        text,
        isAccepted
    )
}