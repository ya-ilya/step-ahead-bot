package me.yailya.step_ahead_bot.handlers

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.deleteMessage
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.getOrNull
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.review.ReviewConversationInputs
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.university.University

suspend fun handleUniversityCallback(
    user: User,
    bot: TelegramBot,
    university: University
) {
    message {
        var extraPointsText = ""

        for (extraPoints in university.extraPoints) {
            extraPointsText -= "\n- ${extraPoints.value} балла за ${extraPoints.key.text}"
        }

        "" - bold { university.name } -
                "\n" - expandableBlockquote { university.description } -
                "\n✔\uFE0F Возможности: " - italic { university.facilities.joinToString { it.text } } -
                "\n\uD83C\uDF10 Веб-сайт ВУЗа: " - url { university.website } -
                "\n\uD83D\uDCCC Локация: " - textLink(university.location) { "Яндекс.Карты" } -
                "\n" - bold { "Информация о бюджетном образовании" } -
                "\n" - "- Бюджетных мест: ${university.budgetInfo.placesCount}" -
                "\n" - "- Средний балл на бюджет: ${university.budgetInfo.averagePoints}" -
                "\n" - "- Самый низкий балл на бюджет: ${university.budgetInfo.minimalPoints}" -
                "\n" - bold { "Информация о платном образовании" } -
                "\n" - "- Платных мест: ${university.paidInfo.placesCount}" -
                "\n" - "- Средняя стоимость: ${university.paidInfo.averagePrice}" -
                "\n" - "- Самая низкая стоимость: ${university.paidInfo.minimalPrice}" -
                "\n" - bold { "В цифрах" } -
                "\n" - "- Студентов: ${university.inNumbers.studentsCount}" -
                "\n" - "- Преподователей: ${university.inNumbers.professorsCount}" -
                "\n" - "- Год основания: ${university.inNumbers.yearOfFoundation}" -
                "\n" - bold { "Дополнительные баллы для поступления" } - extraPointsText -
                "\n" - bold { "Списки поступающих в ${university.shortName}" } -
                "\n" - "Списки поступающих доступны на этом сайте: " - url { university.listOfApplicants } -
                "\n" - bold { "Контакты ${university.shortName}" } -
                "\n" - "- Номер телефона: ${university.contacts.phone}" -
                "\n" - "- Электронная почта: ${university.contacts.email}"
    }.inlineKeyboardMarkup {
        "Специальности" callback "specialities_${university.id}"
        "Отзывы" callback "reviews_${university.id}"
    }.options {
        // photo(...)
        disableWebPagePreview()
    }.send(user, bot)
}

suspend fun handleSpecialitiesCallback(
    user: User,
    bot: TelegramBot,
    university: University
) {
    message {
        "" - bold { "Специальности в ${university.shortName}" } -
                "\n" + "Доступные специальности:" -
                "\n" - expandableBlockquote { university.specialities.joinToString("\n") { "- $it" } }
    }.send(user, bot)
}

suspend fun handleReviewsCallback(
    user: User,
    bot: TelegramBot,
    university: University
) {
    val reviews = ReviewEntity.queriedModelsByUniversity(university)

    message {
        if (reviews.isNotEmpty()) {
            var result = "" - bold { "Отзывы о ${university.shortName}" }

            for (review in reviews) {
                result = result - "\n" - bold { "Отзыв №${review.id}. ${review.rating}/5" } -
                        "\n" - "Положительные стороны:" -
                        "\n" - blockquote { review.pros } -
                        "\n" - "Отрицательные стороны:" -
                        "\n" - blockquote { review.cons } -
                        "\n" - "Комментарий:" -
                        "\n" - blockquote { review.comment }
            }

            result
        } else {
            "" - bold { "Отзывов о ${university.shortName} не найдено" }
        }
    }.inlineKeyboardMarkup {
        "Создать отзыв" callback "create_review_${university.id}"
    }.send(user, bot)
}

suspend fun handleCreateReviewCallback(
    user: User,
    bot: TelegramBot,
    university: University
) {
    bot.inputListener[user] = "create_review_step1_${university.id}"

    val message = message {
        "" - bold { "Оставление отзыва о ${university.shortName}" } -
                "\n" - "Что вам понравилось в данном ВУЗе?"
    }.sendAsync(user, bot).getOrNull()

    reviewInputs[user.id] = ReviewConversationInputs(originMessageId = message!!.messageId)
}

val reviewInputs = mutableMapOf<Long, ReviewConversationInputs>()

suspend fun handleCreateReviewStep1Input(
    user: User,
    bot: TelegramBot,
    university: University,
    input: String,
    messageId: Long
) {
    bot.inputListener[user] = "create_review_step2_${university.id}"

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
    bot.inputListener[user] = "create_review_step3_${university.id}"

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
    bot.inputListener[user] = "create_review_step4_${university.id}"

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

suspend fun handleCreateReviewStep4(user: User, bot: TelegramBot, university: University, rating: Int) {
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

    reviewInputs.remove(user.id)

    editMessageText(currentUserInputs.originMessageId) {
        "Спасибо за ваш отзыв об ${university.shortName}! Номер отзыва: #${review.id}"
    }.send(user, bot)
}