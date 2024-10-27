package me.yailya.step_ahead_bot.university.handlers

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.getOrNull
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.review.inputs.ReviewConversationInputs
import me.yailya.step_ahead_bot.university.University

val reviewInputs = mutableMapOf<Long, ReviewConversationInputs>()

suspend fun handleCreateReviewCallback(
    user: User,
    bot: TelegramBot,
    university: University
) {
    bot.inputListener[user] = "university_create_review_step1_${university.id}"

    val message = message {
        "" - bold { "Оставление отзыва о ${university.shortName}" } -
                "\n" - "Что вам понравилось в данном ВУЗе?"
    }.sendAsync(user, bot).getOrNull()

    reviewInputs[user.id] = ReviewConversationInputs(originMessageId = message!!.messageId)
}

suspend fun handleCreateReviewStep1Input(
    user: User,
    bot: TelegramBot,
    university: University,
    input: String,
    messageId: Long
) {
    bot.inputListener[user] = "university_create_review_step2_${university.id}"

    val message = message {
        "" - "Что вам не понравилось в данном ВУЗе?"
    }.sendAsync(user, bot).getOrNull()

    reviewInputs[user.id]!!.apply {
        inputs.add(input)
        messages.add(messageId)
        messages.add(message!!.messageId)
    }
}

suspend fun handleCreateReviewStep2Input(
    user: User,
    bot: TelegramBot,
    university: University,
    input: String,
    messageId: Long
) {
    bot.inputListener[user] = "university_create_review_step3_${university.id}"

    val message = message {
        "" - "Оставьте комментарий о данном ВУЗе"
    }.sendAsync(user, bot).getOrNull()

    reviewInputs[user.id]!!.apply {
        inputs.add(input)
        messages.add(messageId)
        messages.add(message!!.messageId)
    }
}

suspend fun handleCreateReviewStep3Input(
    user: User,
    bot: TelegramBot,
    university: University,
    input: String,
    messageId: Long
) {
    bot.inputListener[user] = "university_create_review_step4_${university.id}"

    val message = message {
        "" - "Поставьте оценку данному ВУЗу"
    }.inlineKeyboardMarkup {
        "1" callback "create_review_step4_1_${university.id}"
        "2" callback "create_review_step4_2_${university.id}"
        newLine()
        "3" callback "create_review_step4_3_${university.id}"
        "4" callback "create_review_step4_4_${university.id}"
        newLine()
        "5" callback "create_review_step4_5_${university.id}"
    }.sendAsync(user, bot).getOrNull()

    reviewInputs[user.id]!!.apply {
        inputs.add(input)
        messages.add(messageId)
        messages.add(message!!.messageId)
    }
}

suspend fun handleCreateReviewStep4Callback(user: User, bot: TelegramBot, university: University, rating: Int) {
    if (!reviewInputs.containsKey(user.id)) {
        return
    }

    val currentUserInputs = reviewInputs[user.id]!!

    for (message in currentUserInputs.messages) {
        deleteMessage(message).send(user, bot)
    }

    val review = databaseQuery {
        ReviewEntity.new {
            this.userId = user.id
            this.universityId = university.id
            this.pros = currentUserInputs.inputs[0]
            this.cons = currentUserInputs.inputs[1]
            this.comment = currentUserInputs.inputs[2]
            this.rating = rating
        }.toModel()
    }

    bot.inputListener.del(user.id)
    reviewInputs.remove(user.id)

    editMessageText(currentUserInputs.originMessageId) {
        "Спасибо за ваш отзыв об ${university.shortName}! Номер отзыва: #${review.id}"
    }.send(user, bot)
}