package me.yailya.step_ahead_bot.assistant

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.ollama.OllamaChatModel
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.university.UniversityEntity

object Assistant {
    private val chatModel: OllamaChatModel = OllamaChatModel
        .builder()
        .baseUrl("http://ollama:11434")
        .modelName("assistant")
        .build()

    private val universitiesData = StringBuilder()

    val defaultPrompt = """
        Ты являешься ai-ассистентом, который помогает абитуриентам с поступлением в различные вузы Москвы
        Попытайся отвечать как представитель мужского пола.
        Попытайся не использовать форматирование Markdown (жирный шрифт, и т.д)
        Ты встроен в Telegram-бота StepAhead.
        Ты должен отвечать по-русски.
        Ты можешь отвечать лишь на тему вузов.
        В тебя загружена информация о некоторых вузах (МГУ, ВШЭ, МФТИ), старайся придерживаться ее.
    """.trimIndent()

    suspend fun addUniversitiesData() {
        databaseQuery {
            for (university in UniversityEntity.all()) {
                universitiesData.append(
                    """
                                Название университета: ${university.name}
                                Название университета ${university.name} на английском: ${university.nameEn}
                                Краткое название университета ${university.name}: ${university.shortName}
                                Айди университета в базе данных бота ${university.name}: #${university.id.value}
                                
                                ${university.shortName} предлагает следующие возможности (facilities): ${university.facilities.joinToString { "${it.text} (${it.name})" }}
                                ${university.shortName} предлагает широкий спектр специальностей: ${university.specialities.joinToString()}

                                Номер телефона ${university.shortName}: ${university.contacts.phone}
                                Электронная почта ${university.shortName}: ${university.contacts.email}

                                ВКонтакте ${university.shortName}: ${university.socialNetworks.vk}
                                Telegram ${university.shortName}: ${university.socialNetworks.tg}

                                Количество студентов в ${university.inNumbers.year} в ${university.shortName}: ${university.inNumbers.studentsCount}
                                Количество преподавателей в ${university.inNumbers.year} в ${university.shortName}: ${university.inNumbers.professorsCount}

                                Ссылка на список поступающих в ${university.shortName}: ${university.listOfApplicants}
                            """.trimIndent()
                )
            }
        }
    }

    fun generateResponse(text: String, messages: MutableList<ChatMessage>): String? {
        try {
            messages.add(
                index = 0,
                AiMessage.from("Data about universities from database: $universitiesData")
            )

            messages.add(
                UserMessage.from(text)
            )

            val response = chatModel
                .generate(messages)
                .content()

            messages.add(response)

            return response.text()
        } catch (ex: Exception) {
            println("Error when creating response: ${ex.message}")
        } finally {
            messages.removeAt(0)
        }

        return null
    }
}