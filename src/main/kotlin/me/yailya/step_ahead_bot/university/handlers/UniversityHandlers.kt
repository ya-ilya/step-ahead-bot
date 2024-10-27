package me.yailya.step_ahead_bot.university.handlers

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.ranking.EduRankRanking

suspend fun handleUniversitiesCallback(
    user: User,
    bot: TelegramBot
) {
    message { "Приветствуем вас! Выберете один из ВУЗов:" }.inlineKeyboardMarkup {
        for (university in Universities) {
            if (university.key % 4 == 0) {
                newLine()
            }

            "(${university.key}) ${university.value.shortName}" callback "university_${university.key}"
        }
    }.send(user, bot)
}

suspend fun handleUniversityCallback(
    user: User,
    bot: TelegramBot,
    university: University
) {
    val universityRankData = EduRankRanking.ranking[university.name_en]!!

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
                "\n" - "- Ранг в Москве: " - textLink(universityRankData.rankingUrl) { "#${universityRankData.rankInMoscow}" } -
                "\n" - "- Студентов: ${university.inNumbers.studentsCount}" -
                "\n" - "- Преподователей: ${university.inNumbers.professorsCount}" -
                "\n" - "- Год основания: ${university.inNumbers.yearOfFoundation}" -
                "\n" - bold { "Дополнительные баллы для поступления" } - extraPointsText -
                "\n" - bold { "Списки поступающих в ${university.shortName}" } -
                "\n" - "Списки поступающих доступны на этом сайте: " - url { university.listOfApplicants } -
                "\n" - bold { "Контакты ${university.shortName}" } -
                "\n" - "- Номер телефона: ${university.contacts.phone}" -
                "\n" - "- Электронная почта: ${university.contacts.email}" -
                "\n" - bold { "Социальные сети: " } - textLink(university.socialNetworks.vk) { "ВКонтакте" } - ", " - textLink(
            university.socialNetworks.tg
        ) { "Telegram" }
    }.inlineKeyboardMarkup {
        "Специальности" callback "university_specialities_${university.id}"
        "Отзывы" callback "university_reviews_${university.id}"
        newLine()
        "Создать запрос на изменение информации" callback "university_create_update_request_${university.id}"
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
    val reviews = ReviewEntity.getModelsByUniversity(university)

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
        "Создать отзыв" callback "university_create_review_${university.id}"
    }.send(user, bot)
}