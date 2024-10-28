@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.queries.callback.CallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.Dispatchers
import me.yailya.step_ahead_bot.commands.handleFaqCommand
import me.yailya.step_ahead_bot.commands.handleModerateCommand
import me.yailya.step_ahead_bot.commands.handleStartCommand
import me.yailya.step_ahead_bot.moderator.ModeratorEntity
import me.yailya.step_ahead_bot.moderator.Moderators
import me.yailya.step_ahead_bot.moderator.handlers.handleModerateUpdateRequestCallback
import me.yailya.step_ahead_bot.moderator.handlers.handleModerateUpdateRequestCloseCallback
import me.yailya.step_ahead_bot.moderator.handlers.handleModerateUpdateRequestCloseDoneCallback
import me.yailya.step_ahead_bot.review.Reviews
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.university.handlers.*
import me.yailya.step_ahead_bot.university.ranking.EduRankRanking
import me.yailya.step_ahead_bot.update_request.UpdateRequests
import me.yailya.step_ahead_bot.update_request.handlers.handleUpdateRequestsCallback
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun BehaviourContext.reply(
    to: CallbackQuery,
    text: String,
    linkPreviewOptions: LinkPreviewOptions? = null,
    replyMarkup: KeyboardMarkup? = null
) {
    reply(
        to = to.message!!,
        text = text,
        linkPreviewOptions = linkPreviewOptions,
        replyMarkup = replyMarkup
    )
}

suspend fun BehaviourContext.reply(
    to: CallbackQuery,
    entities: TextSourcesList,
    linkPreviewOptions: LinkPreviewOptions? = null,
    replyMarkup: KeyboardMarkup? = null
) {
    reply(
        to = to.message!!,
        entities = entities,
        linkPreviewOptions = linkPreviewOptions,
        replyMarkup = replyMarkup
    )
}

suspend fun main() {
    println("Loaded EduRank ranking. ${EduRankRanking.ranking.size} entries")

    val driverClassName = "org.h2.Driver"
    val jdbcURL = "jdbc:h2:file:./database"
    val database = Database.connect(jdbcURL, driverClassName)

    transaction(database) {
        SchemaUtils.create(Reviews, UpdateRequests, Moderators)

        addLogger(StdOutSqlLogger)
    }

    databaseQuery {
        if (ModeratorEntity.all().empty()) {
            ModeratorEntity.new {
                this.userId = 1005465506
            }
        }
    }

    val bot = telegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

    bot.buildBehaviourWithLongPolling {
        onCommand("start") {
            this.handleStartCommand(it)
        }

        onCommand("faq") {
            this.handleFaqCommand(it)
        }

        onCommand("moderate") {
            this.handleModerateCommand(it)
        }

        onDataCallbackQuery("universities") {
            this.handleUniversitiesCallback(it)
            answerCallbackQuery(it)
        }

        onDataCallbackQuery("update_requests") {
            this.handleUpdateRequestsCallback(it)
            answerCallbackQuery(it)
        }

        onDataCallbackQuery("moderate_update_requests") {
            this.handleModerateUpdateRequestCallback(it, -1)
            answerCallbackQuery(it)
        }

        val universityRegex = "university(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(universityRegex) {
            val values = universityRegex.find(it.data)!!.groupValues
            val name = values[1]
            val university = Universities[values[2].toInt()]

            when {
                name.isEmpty() -> {
                    this.handleUniversityCallback(it, university)
                }

                name == "specialities" -> {
                    this.handleSpecialitiesCallback(it, university)
                }

                name == "reviews" -> {
                    this.handleReviewsCallback(it, university)
                }

                name == "create_review" -> {
                    this.handleCreateReviewCallback(it, university)
                }

                name == "create_update_request" -> {
                    this.handleCreateUpdateRequestCallback(it, university)
                }
            }

            answerCallbackQuery(it)
        }

        val moderateUpdateRequestRegex = "moderate_update_request(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(moderateUpdateRequestRegex) {
            val values = moderateUpdateRequestRegex.find(it.data)!!.groupValues
            val name = values[1]
            val updateRequestId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleModerateUpdateRequestCallback(it, updateRequestId)
                }

                name == "close" -> {
                    this.handleModerateUpdateRequestCloseCallback(it, updateRequestId)
                }

                name == "close_done" -> {
                    this.handleModerateUpdateRequestCloseDoneCallback(it, updateRequestId)
                }
            }

            answerCallbackQuery(it)
        }
    }.join()
}

suspend fun <T> databaseQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }