package me.yailya.step_ahead_bot.question.answer

import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.question.QuestionEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class AnswerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AnswerEntity>(Answers)

    var botUser by BotUserEntity referencedOn Answers.botUser
    var question by QuestionEntity referencedOn Answers.question
    var text by Answers.text
    var isAccepted by Answers.isAccepted

    fun toModel() = Answer(
        id.value,
        botUser.toModel(),
        question.toModel(),
        text,
        isAccepted
    )
}