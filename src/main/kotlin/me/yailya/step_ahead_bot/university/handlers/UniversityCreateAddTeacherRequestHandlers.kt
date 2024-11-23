@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.send.SendTextMessage
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
import me.yailya.step_ahead_bot.teacher.TeacherAcademicTitle
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequestEntity
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequestStatus
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.UniversityEntity
import java.time.LocalDateTime

suspend fun BehaviourContext.handleCreateAddTeacherRequestCallback(
    query: DataCallbackQuery,
    university: University
) {
    val (botUserEntity, botUser) = query.botUser()

    if (botUser.lastAddTeacherRequestTime != null && LocalDateTime.now() < botUser.lastAddTeacherRequestTime.plusMinutes(
            1
        )
    ) {
        answerCallbackQuery(
            query,
            "⏳ Вы должны подождать минуту, прежде чем создать новый запрос на добавление нового преподавателя"
        )

        return
    }

    val fullNameMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +bold("${university.shortName} -> Создание запроса на изменение информации") +
                        "\n" + "Введите ФИО преподавателя:"
            }
        )
    ).first()

    val experienceMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +"Введите опыт работы преподавателя:"
            }
        )
    ).first()

    val academicTitleQuery = waitDataCallbackQuery(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +"Выберете должность преподавателя:"
            },
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("Доцент", "Docent")
                }

                row {
                    dataButton("Профессор", "Professor")
                }
            }
        )
    ).first()

    answerCallbackQuery(academicTitleQuery)

    editInlineButton(
        academicTitleQuery,
        { button -> button.text.contains(academicTitleQuery.data) },
        { button -> dataInlineButton("✅ ${button.text}", academicTitleQuery.data) }
    )

    val specialitiesMessage = waitTextMessage(
        SendTextMessage(
            query.message!!.chat.id,
            buildEntities {
                +"Введите специальности преподавателя (через запятую):"
            }
        )
    ).first()

    val addTeacherRequest = databaseQuery {
        botUserEntity.lastAddTeacherRequestTime = LocalDateTime.now()

        AddTeacherRequestEntity.new {
            this.botUser = botUserEntity
            this.university = UniversityEntity.findById(university.id)!!
            this.fullName = fullNameMessage.content.text
            this.experience = experienceMessage.content.text.toInt()
            this.academicTitle = TeacherAcademicTitle.valueOf(academicTitleQuery.data)
            this.specialities = specialitiesMessage.content.text.split(",").map { it.trim() }
            this.status = AddTeacherRequestStatus.Open
        }.toModel()
    }

    reply(
        to = fullNameMessage,
        text = "✅ Спасибо за ваше запрос на добавление преподавателя ${university.shortName}. В скором времени он будет рассмотрен модерацией. Номер запроса: #${addTeacherRequest.id}"
    )

    answerCallbackQuery(query)
}