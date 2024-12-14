package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.message.textsources.italic
import dev.inmo.tgbotapi.types.message.textsources.link
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.EntitiesBuilder
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.expandableBlockquote
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.ranking.EduRankRanking

suspend fun BehaviourContext.handleUniversityCallback(query: DataCallbackQuery, university: University) {
    val universityRankData = EduRankRanking.ranking[university.nameEn]!!

    reply(
        to = query,
        buildEntities {
            var extraPointsText = EntitiesBuilder()

            for (extraPoints in university.extraPoints) {
                extraPointsText += "\n- ${extraPoints.value} балла за ${extraPoints.key.text}"
            }

            +bold(university.name) +
                    "\n" + expandableBlockquote(university.description) +
                    "\n✔\uFE0F Возможности: " + italic(university.facilities.joinToString { it.text }) +
                    "\n\uD83C\uDF10 Веб-сайт ВУЗа: " + link(university.website) +
                    "\n\uD83D\uDCCC Локация: " + link("Яндекс.Карты", university.location) +
                    "\n" + bold("Информация о бюджетном образовании") +
                    "\n" + "- Бюджетных мест: ${university.budgetInfo.placesCount}" +
                    "\n" + "- Средний балл на бюджет: ${university.budgetInfo.averagePoints}" +
                    "\n" + "- Самый низкий балл на бюджет: ${university.budgetInfo.minimalPoints}" +
                    "\n" + bold("Информация о платном образовании") +
                    "\n" + "- Платных мест: ${university.paidInfo.placesCount}" +
                    "\n" + "- Средняя стоимость: ${university.paidInfo.averagePrice}" +
                    "\n" + "- Самая низкая стоимость: ${university.paidInfo.minimalPrice}" +
                    "\n" + bold("В цифрах") +
                    "\n" + "- Ранг в Москве: " + link(
                "#${universityRankData.rankInMoscow}",
                universityRankData.rankingUrl
            ) +
                    "\n" + "- Студентов: ${university.inNumbers.studentsCount}" +
                    "\n" + "- преподавателей: ${university.inNumbers.professorsCount}" +
                    "\n" + "- Год основания: ${university.inNumbers.yearOfFoundation}" +
                    "\n" + bold("Дополнительные баллы для поступления") + extraPointsText +
                    "\n" + bold("Списки поступающих в ${university.shortName}") +
                    "\n" + "Списки поступающих доступны на этом сайте: " + link(university.listOfApplicants) +
                    "\n" + bold("Контакты ${university.shortName}") +
                    "\n" + "- Номер телефона: ${university.contacts.phone}" +
                    "\n" + "- Электронная почта: ${university.contacts.email}" +
                    "\n" + bold("Социальные сети: ") + link(
                "ВКонтакте",
                university.socialNetworks.vk
            ) + ", " + link("Telegram", university.socialNetworks.tg)
        },
        linkPreviewOptions = LinkPreviewOptions.Disabled,
        replyMarkup = inlineKeyboard {
            row {
                dataButton(
                    "\uD83C\uDF93 Специальности",
                    "university_specialities_${university.id}"
                )
            }

            row {
                dataButton(
                    "\uD83D\uDC69\u200D\uD83C\uDFEB Преподаватели",
                    "university_Teachers_${university.id}"
                )
            }

            row {
                dataButton(
                    "\uD83C\uDFC6 Олимпиады",
                    "university_Olympiads_${university.id}"
                )
            }

            row {
                dataButton(
                    "❔ Вопросы",
                    "university_Questions_${university.id}"
                )
                dataButton(
                    "✍\uD83C\uDFFB Задать вопрос",
                    "university_create_Question_${university.id}"
                )
            }

            row {
                dataButton(
                    "⭐ Отзывы${if (university.averageRating > 0) " (${university.averageRating}/5.0)" else ""}",
                    "university_UniversityReviews_${university.id}"
                )
                dataButton(
                    "✍\uD83C\uDFFB Создать отзыв",
                    "university_create_UniversityReview_${university.id}"
                )
            }

            row {
                dataButton(
                    "✍\uD83C\uDFFB Создать запрос на добавление нового преподавателя",
                    "university_create_AddTeacherRequest_${university.id}"
                )
            }

            row {
                dataButton(
                    "✍\uD83C\uDFFB Создать запрос на изменение информации",
                    "university_create_UniversityUpdateRequest_${university.id}"
                )
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.universityHandleSpecialitiesCallback(
    query: DataCallbackQuery,
    university: University
) {
    reply(
        to = query,
        buildEntities {
            +"" + bold("Специальности в ${university.shortName}") +
                    "\n" + "Доступные специальности:" +
                    "\n" + expandableBlockquote(university.specialities.joinToString("\n") { "- $it" })
        }
    )

    answerCallbackQuery(query)
}