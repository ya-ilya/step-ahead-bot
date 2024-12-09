package me.yailya.step_ahead_bot.assistant

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.university.UniversityEntity

object Assistant {
    private val embeddingStore = InMemoryEmbeddingStore<TextSegment>()

    private val embeddingModel: OllamaEmbeddingModel = OllamaEmbeddingModel
        .builder()
        .baseUrl("http://ollama:11434")
        .modelName("mxbai-embed-large")
        .build()

    private val chatModel: OllamaChatModel = OllamaChatModel
        .builder()
        .baseUrl("http://ollama:11434")
        .modelName("assistant")
        .build()

    suspend fun addUniversitiesEmbeddingData() {
        databaseQuery {
            for (university in UniversityEntity.all()) {
                val segment = TextSegment.from(
                    """
                                Название университета: ${university.name}
                                Название университета на английском: ${university.nameEn}
                                Краткое название университета: ${university.shortName}
                                Айди университета в базе данных бота ${university.name}: #${university.id.value}

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
                val embedding = embeddingModel.embed(segment).content()
                embeddingStore.add(embedding, segment)
            }
        }
    }

    fun generateResponse(text: String, messages: MutableList<ChatMessage>): String? {
        try {
            val queryEmbedding = embeddingModel
                .embed(text)
                .content()

            val relevant = embeddingStore.search(
                EmbeddingSearchRequest
                    .builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(1)
                    .build()
            ).matches()

            val embeddingMatch = relevant[0]
                .embedded()
                .text()

            messages.add(
                AiMessage.from("This is additional data for my next response. $embeddingMatch")
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
        }

        return null
    }
}