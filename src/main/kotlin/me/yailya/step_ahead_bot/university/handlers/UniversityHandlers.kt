package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.message.textsources.italic
import dev.inmo.tgbotapi.types.message.textsources.link
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.EntitiesBuilder
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.expandableBlockquote
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.ranking.EduRankRanking

suspend fun BehaviourContext.handleUniversitiesCallback(query: DataCallbackQuery) {
    reply(
        to = query,
        text = "Приветствуем вас! Выберете один из ВУЗов:",
        replyMarkup = inlineKeyboard {
            for (chunk in Universities.iterator().asSequence().toList().chunked(4)) {
                row {
                    for (university in chunk) {
                        dataButton("(${university.key}) ${university.value.shortName}", "university_${university.key}")
                    }
                }
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleUniversityCallback(query: DataCallbackQuery, university: University) {
    val universityRankData = EduRankRanking.ranking[university.name_en]!!

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
                    "\n" + "- Преподователей: ${university.inNumbers.professorsCount}" +
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
                dataButton("Специальности", "university_specialities_${university.id}")
                dataButton("Отзывы", "university_reviews_${university.id}")
            }

            row {
                dataButton(
                    "Создать отзыв",
                    "university_create_review_${university.id}"
                )
            }

            row {
                dataButton(
                    "Создать запрос на изменение информации",
                    "university_create_update_request_${university.id}"
                )
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleUniversitySpecialitiesCallback(
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

suspend fun BehaviourContext.handleUniversityReviewCallback(
    query: DataCallbackQuery,
    reviewId: Int,
    university: University
) {
    val reviews = ReviewEntity.getModelsByUniversity(university)

    if (reviews.isEmpty()) {
        answerCallbackQuery(
            query,
            "Отзывов о ${university.shortName} не найдено"
        )

        return
    }

    val realReviewId = if (reviewId == -1) reviews.first().id else reviewId
    val review = reviews.find { it.id == realReviewId }

    if (review == null) {
        answerCallbackQuery(
            query,
            "Данного отзыва (#${reviewId}) не существует, либо же он принадлежит другому ВУЗу"
        )

        return
    }

    val reviewIndex = reviews.indexOf(review)
    val previousReviewId = reviews.elementAtOrNull(reviewIndex - 1).let { it?.id ?: -1 }
    val nextReviewId = reviews.elementAtOrNull(reviewIndex + 1).let { it?.id ?: -1 }

    reply(
        to = query,
        buildEntities {
            +bold("Отзыв №${review.id}. ${review.rating}/5") +
                    "\n" + "Положительные стороны:" +
                    "\n" + blockquote(review.pros) +
                    "\n" + "Отрицательные стороны:" +
                    "\n" + blockquote(review.cons) +
                    "\n" + "Комментарий:" +
                    "\n" + blockquote(review.comment)
        },
        replyMarkup = inlineKeyboard {
            if (previousReviewId != -1) {
                row {
                    dataButton("Предыдущий", "university_review_${previousReviewId}_${university.id}")
                }
            }
            if (nextReviewId != -1) {
                row {
                    dataButton("Следущий", "university_review_${nextReviewId}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}