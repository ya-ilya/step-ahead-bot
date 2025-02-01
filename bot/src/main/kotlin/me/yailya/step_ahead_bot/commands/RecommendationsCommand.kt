package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.row
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import me.yailya.step_ahead_bot.assistant.Assistant
import me.yailya.step_ahead_bot.assistant.AssistantQueue
import me.yailya.step_ahead_bot.university.University

enum class Subject(val text: String) {
    RUSSIAN_LANGUAGE("Русский язык"),
    MATHEMATICS("Математика"),
    PHYSICS("Физика"),
    CHEMISTRY("Химия"),
    BIOLOGY("Биология"),
    LITERATURE("Литература"),
    GEOGRAPHY("География"),
    HISTORY("История"),
    SOCIAL_STUDIES("Обществознание"),
    ENGLISH("Английский"),
    FRENCH("Французский"),
    GERMAN("Немецкий"),
    SPANISH("Испанский"),
    COMPUTER_SCIENCE("Информатика");
}

private val queue = AssistantQueue(maxSize = System.getenv("ASSISTANT_QUEUE_MAX_SIZE").toInt())

suspend fun BehaviourContext.handleRecommendationsCommand(message: TextMessage) {
    val userId = message.chat.id.chatId.long

    val history = mutableListOf<ChatMessage>(AiMessage.from(Assistant.defaultPrompt))

    val subjects = mutableMapOf<Subject, Int>()

    fun buildSubjectsReplyMarkup() = inlineKeyboard {
        for (chunk in Subject.entries.chunked(2)) {
            row {
                for (subject in chunk) {
                    if (subjects.containsKey(subject)) {
                        dataButton("✅ ${subject.text} (${subjects[subject]})", subject.name)
                    } else {
                        dataButton(subject.text, subject.name)
                    }
                }
            }
        }

        row {
            dataButton("Следующий шаг ➡\uFE0F", "next")
        }
    }

    val message1 = reply(
        message,
        entities = buildEntities {
            +bold("Получение личных рекомендаций -> Шаг 1. Баллы ЕГЭ")
            +"\nДобавьте ваши результаты ЕГЭ"
        },
        replyMarkup = buildSubjectsReplyMarkup()
    )

    val callbackQueryFlow = waitDataCallbackQuery()

    callbackQueryFlow
        .takeWhile { Subject.entries.any { subject -> subject.name == it.data } }
        .collect { query ->
            val subject = Subject.valueOf(query.data)

            val pointsMessage = waitTextMessage(
                SendTextMessage(
                    message1.chat.id,
                    "Введите количество баллов по предмету ${subject.text}"
                )
            ).first()

            try {
                val pointsInteger = pointsMessage.content.text.toInt()

                if (pointsInteger < 0 || pointsInteger > 100) {
                    throw IllegalArgumentException()
                }

                subjects[subject] = pointsInteger

                edit(
                    message1,
                    replyMarkup = buildSubjectsReplyMarkup()
                )

                reply(
                    pointsMessage,
                    "✅ Баллы по предмету ${subject.text} были сохранены"
                )
            } catch (ex: Exception) {
                reply(
                    pointsMessage,
                    "❌ Вы ввели неправильное количество баллов. Это должно быть число от 0 до 100"
                )
            }

            answerCallbackQuery(query)
        }

    callbackQueryFlow.firstOrNull()?.also {
        if (it.data == "next") {
            answerCallbackQuery(it)
        }
    }

    val facilities = mutableSetOf<University.UniversityFacility>()

    fun buildFacilitiesReplyMarkup() = inlineKeyboard {
        for (chunk in University.UniversityFacility.entries.chunked(2)) {
            row {
                for (facility in chunk) {
                    if (facilities.contains(facility)) {
                        dataButton("✅ ${facility.text}", facility.name)
                    } else {
                        dataButton(facility.text, facility.name)
                    }
                }
            }
        }

        row {
            dataButton("Следующий шаг ➡\uFE0F", "next")
        }
    }

    val message2 = reply(
        message1,
        entities = buildEntities {
            +bold("Получение личных рекомендаций -> Шаг 2. Баллы ЕГЭ")
            +"\nВыберите возможности университета, которые вам нужны"
        },
        replyMarkup = buildFacilitiesReplyMarkup()
    )

    callbackQueryFlow
        .takeWhile { University.UniversityFacility.entries.any { facility -> facility.name == it.data } }
        .collect { query ->
            val facility = University.UniversityFacility.valueOf(query.data)

            if (facilities.contains(facility)) {
                facilities.remove(facility)

                reply(
                    message2,
                    "✅ Возможность ${facility.text} больше не выбрана"
                )
            } else {
                facilities.add(facility)

                reply(
                    message2,
                    "✅ Возможность ${facility.text} была выбрана"
                )
            }

            edit(
                message2,
                replyMarkup = buildFacilitiesReplyMarkup()
            )

            answerCallbackQuery(query)
        }

    callbackQueryFlow.firstOrNull()?.also {
        if (it.data == "next") {
            answerCallbackQuery(it)
        }
    }

    val message3 = reply(
        message2,
        entities = buildEntities {
            +bold("Получение личных рекомендаций -> Шаг 3. Личные предпочтения")
            +"\nКратко опишите ваши личные предпочтения"
        },
    )

    val personalPreferences = waitText().first().text

    if (queue.isFull()) {
        val text = { "⏳ Ожидание в очереди (ваше место: ${queue.getUserPosition(userId)}..." }
        val queueMessage = reply(
            to = message3,
            text = text()
        )

        runBlocking {
            while (queue.isFull()) {
                if (!queueMessage.content.text.contentEquals(text())) {
                    editMessageText(
                        queueMessage,
                        text()
                    )
                }
                delay(3000)
            }

            deleteMessage(queueMessage)
        }
    } else {
        queue.addUser(userId)
    }

    try {
        val waitMessage = reply(
            to = message3,
            text = "⏳ Генерация ответа..."
        )

        val response = Assistant.generateResponse(
            """
            Составь список личных рекомендаций, используя только вузы, о которых у тебя есть информация из базы данных,
            и дай ответ в виде списка по шаблону "ID университета: Почему была дана такая рекомендация". Для создания
            личных рекомендаций используй следующие данные, предоставленные пользователем.
            Попытайся выдать максимальное количество вузов, где есть подходящие специальности: 
            - Баллы ЕГЭ: ${subjects.map { "${it.key.text} - ${it.value}" }.joinToString()}
            - Нужные возможности (facilities) университета: ${facilities.joinToString { "${it.text} (${it.name})" }}
            - Личные предпочтения: ```$personalPreferences```
        """.trimIndent(), history
        )

        if (response != null) {
            edit(
                waitMessage,
                text = response
            )
        } else {
            edit(
                waitMessage,
                text = "❌ Произошла ошибка при генерации ответа. Попробуйте еще раз, или свяжитесь с администрацией"
            )
        }
    } finally {
        queue.removeUser(userId)
    }
}