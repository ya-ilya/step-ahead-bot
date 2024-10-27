package me.yailya.step_ahead_bot

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.answer.answerCallbackQuery
import eu.vendeli.tgbot.types.internal.getUser
import eu.vendeli.tgbot.utils.onCallbackQuery
import kotlinx.coroutines.Dispatchers
import me.yailya.step_ahead_bot.commands.handleFAQCommand
import me.yailya.step_ahead_bot.commands.handleStartCommand
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

suspend fun main() {
    println("Loaded EduRank ranking. ${EduRankRanking.ranking.size} entries")

    val driverClassName = "org.h2.Driver"
    val jdbcURL = "jdbc:h2:file:./database"
    val database = Database.connect(jdbcURL, driverClassName)

    transaction(database) {
        SchemaUtils.create(Reviews, UpdateRequests)

        addLogger(StdOutSqlLogger)
    }

    val bot = TelegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

    bot.handleUpdates {
        onCommand("/start") {
            handleStartCommand(user, bot)
        }

        onCommand("/faq") {
            handleFAQCommand(user, bot)
        }

        for (university in Universities) {
            onInput("create_review_step1_${university.key}") {
                handleCreateReviewStep1Input(
                    update.getUser(),
                    bot,
                    university.value,
                    update.text,
                    update.origin.message!!.messageId
                )
            }

            onInput("create_review_step2_${university.key}") {
                handleCreateReviewStep2Input(
                    update.getUser(),
                    bot,
                    university.value,
                    update.text,
                    update.origin.message!!.messageId
                )
            }

            onInput("create_review_step3_${university.key}") {
                handleCreateReviewStep3Input(
                    update.getUser(),
                    bot,
                    university.value,
                    update.text,
                    update.origin.message!!.messageId
                )
            }

            onInput("create_update_request_step1_${university.key}") {
                handleCreateUpdateRequestStep1Input(
                    update.getUser(),
                    bot,
                    university.value,
                    update.text,
                    update.origin.message!!.messageId
                )
            }
        }

        onCallbackQuery {
            if (update.callbackQuery.data!!.last().isDigit()) {
                val university = Universities[update.callbackQuery.data!!.split("_").last().toInt()]

                when {
                    update.callbackQuery.data!!.startsWith("university_") -> {
                        handleUniversityCallback(update.user, bot, university)
                    }

                    update.callbackQuery.data!!.contains("_") -> {
                        val callbackNameSplit = update.callbackQuery.data!!
                            .split("_")
                            .dropLast(1)

                        when (val callbackName = callbackNameSplit.joinToString("_")) {
                            "specialities" -> handleSpecialitiesCallback(update.user, bot, university)
                            "reviews" -> handleReviewsCallback(update.user, bot, university)
                            "create_review" -> handleCreateReviewCallback(update.user, bot, university)
                            "create_update_request" -> handleCreateUpdateRequestCallback(update.user, bot, university)
                            else -> when {
                                callbackName.startsWith("create_review_step4_") -> {
                                    handleCreateReviewStep4Callback(update.user, bot, university, callbackNameSplit.last().toInt())
                                }
                            }
                        }
                    }
                }
            } else {
                when (update.callbackQuery.data) {
                    "universities" -> handleUniversitiesCallback(update.user, bot)
                    "update_requests" -> handleUpdateRequestsCallback(update.user, bot)
                }
            }

            answerCallbackQuery(update.callbackQuery.id).send(update.user, bot)
        }
    }
}

suspend fun <T> databaseQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }