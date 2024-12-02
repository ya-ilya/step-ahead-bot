@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ReplyParameters
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import me.yailya.step_ahead_bot.bot_user.botUser
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.editInlineButton
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.UniversityEntity
import me.yailya.step_ahead_bot.university.review.UniversityReviewEntity
import java.time.LocalDateTime

suspend fun BehaviourContext.universityHandleCreateUniversityReviewCallback(
    query: DataCallbackQuery,
    university: University
) {
    val (botUserEntity, botUser) = query.botUser()

    if (botUser.lastReviewTime != null && LocalDateTime.now() < botUser.lastReviewTime.plusMinutes(1)) {
        answerCallbackQuery(
            query,
            "⏳ Вы должны подождать минуту, прежде чем оставить новый отзыв"
        )

        return
    }

    val prosMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("${university.shortName} -> Создание отзыва") +
                        "\n" + "Что вам понравилось в данном ВУЗе?"
            },
            replyParameters = ReplyParameters(metaInfo = query.message!!.metaInfo)
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

    val ratingQuery = waitDataCallbackQuery(
        SendTextMessage(
            query.message!!.chat.id,
            "Поставьте оценку данному ВУЗу",
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("1\uFE0F⃣", "1")
                    dataButton("2\uFE0F⃣", "2")
                }
                row {
                    dataButton("3\uFE0F⃣", "3")
                    dataButton("4\uFE0F⃣", "4")
                }
                row {
                    dataButton("5\uFE0F⃣", "5")
                }
            }
        )
    ).first()

    answerCallbackQuery(ratingQuery)

    editInlineButton(
        ratingQuery,
        { button -> button.text.contains(ratingQuery.data) },
        { button -> dataInlineButton("✅ ${button.text}", ratingQuery.data) }
    )

    val rating = ratingQuery.data.toInt()

    val review = databaseQuery {
        botUserEntity.lastReviewTime = LocalDateTime.now()

        UniversityReviewEntity.new {
            this.botUser = botUserEntity
            this.university = UniversityEntity.findById(university.id)!!
            this.pros = prosMessage.content.text
            this.cons = cons
            this.comment = comment
            this.rating = rating
        }.toModel()
    }

    reply(
        to = prosMessage,
        text = "✅ Спасибо за ваш отзыв об ${university.shortName}! Номер отзыва: #${review.id}"
    )

    answerCallbackQuery(query)
}