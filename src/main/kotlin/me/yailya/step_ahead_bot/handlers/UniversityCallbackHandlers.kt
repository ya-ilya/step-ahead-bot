package me.yailya.step_ahead_bot.handlers

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.answer.answerCallbackQuery
import eu.vendeli.tgbot.api.answer.answerInlineQuery
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.CallbackQuery
import eu.vendeli.tgbot.types.User
import me.yailya.step_ahead_bot.university.UniversityModel

suspend fun handleUniversityCallback(user: User, bot: TelegramBot, university: UniversityModel) {
    message {
        var extraPointsText = ""

        for (extraPoints in university.extraPoints) {
            extraPointsText = extraPointsText.minus("\n- ${extraPoints.value} балла за ${extraPoints.key.text}")
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

suspend fun handleSpecialitiesCallback(user: User, bot: TelegramBot, university: UniversityModel) {
    message {
        "" - bold { "Специальности в ${university.shortName}" } -
                "\n" + "Доступные специальности:" -
                "\n" - expandableBlockquote { university.specialities.joinToString("\n") { "- $it" } }
    }.send(user, bot)
}

suspend fun handleReviewsCallback(user: User, bot: TelegramBot, university: UniversityModel) {
    message {
        "" - bold { "Отзывы о ${university.shortName}" }
    }.send(user, bot)
}