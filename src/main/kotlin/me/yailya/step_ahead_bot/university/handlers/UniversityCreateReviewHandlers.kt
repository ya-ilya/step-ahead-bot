@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitCallbackQueries
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.university.University

@RiskFeature
suspend fun BehaviourContext.handleCreateReviewCallback(
    query: DataCallbackQuery,
    university: University
) {
    val prosMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                "" + bold("Оставление отзыва о ${university.shortName}") +
                        "\n" + "Что вам понравилось в данном ВУЗе?"
            }
        )
    ).first()

    val cons = waitText(
        SendTextMessage(
            query.message!!.chat.id,
            "Что вам не понравилось в данном ВУЗе?"
        )
    ).first().text

    val comment = waitText(
        SendTextMessage(
            query.message!!.chat.id,
            "Оставьте комментарий о данном ВУЗе"
        )
    ).first().text

    val rating = waitCallbackQueries<DataCallbackQuery>(
        SendTextMessage(
            query.message!!.chat.id,
            "Поставьте оценку данному ВУЗу",
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("1", "create_review_step4_1_${university.id}")
                    dataButton("2", "create_review_step4_2_${university.id}")
                }
                row {
                    dataButton("3", "create_review_step4_3_${university.id}")
                    dataButton("4", "create_review_step4_4_${university.id}")
                }
                row {
                    dataButton("5", "create_review_step4_5_${university.id}")
                }
            }
        )
    ).first().data.split("_").dropLast(1).last().toInt()

    val review = databaseQuery {
        ReviewEntity.new {
            this.userId = query.user.id.chatId.long
            this.universityId = university.id
            this.pros = prosMessage.content.text
            this.cons = cons
            this.comment = comment
            this.rating = rating
        }.toModel()
    }

    reply(
        to = prosMessage,
        text = "Спасибо за ваш отзыв об ${university.shortName}! Номер отзыва: #${review.id}"
    )
}