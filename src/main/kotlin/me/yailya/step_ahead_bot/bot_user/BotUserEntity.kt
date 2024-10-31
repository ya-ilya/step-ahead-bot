package me.yailya.step_ahead_bot.bot_user

import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.review.Reviews
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity
import me.yailya.step_ahead_bot.update_request.UpdateRequests
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BotUserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BotUserEntity>(BotUsers)

    var userId by BotUsers.userId
    var isModerator by BotUsers.isModerator

    val reviews by ReviewEntity referrersOn Reviews.botUser
    val updateRequests by UpdateRequestEntity referrersOn UpdateRequests.botUser

    fun toModel() = BotUser(
        id.value,
        userId,
        isModerator
    )
}