package me.yailya.step_ahead_bot.assistant

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore
import java.io.File

object Assistant {
    var isLoaded: Boolean = false

    private val embeddingStore = ChromaEmbeddingStore
        .builder()
        .baseUrl("http://chromadb:8000")
        .collectionName("universities")
        .build()

    private val embeddingModel: OllamaEmbeddingModel = OllamaEmbeddingModel
        .builder()
        .baseUrl("http://ollama:11434")
        .modelName("mxbai-embed-large")
        .build()

    private val chatModel: OllamaChatModel = OllamaChatModel
        .builder()
        .baseUrl("http://ollama:11434")
        .modelName("llama3.1:8b")
        .build()

    init {
        try {
            for (file in File("/app/documents/").listFiles()!!) {
                val segment = TextSegment.from(file.readText())
                val embedding = embeddingModel.embed(segment).content()
                embeddingStore.add(embedding, segment)
            }

            isLoaded = true
        } catch (ex: Exception) {
            isLoaded = false
            throw ex
        }
    }


    fun generateResponse(text: String, messages: MutableList<ChatMessage>): String {
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
                UserMessage.from("Using this data: ${embeddingMatch}. Respond to this prompt: $text")
            )

            return chatModel
                .generate(messages)
                .content()
                .text()
        } catch (ex: Exception) {
            println("Error when creating response: ${ex.message}")
        }

        return "Error has been occurred"
    }
}