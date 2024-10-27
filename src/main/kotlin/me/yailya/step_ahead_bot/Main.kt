package me.yailya.step_ahead_bot

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.answer.answerCallbackQuery
import eu.vendeli.tgbot.types.internal.getUser
import eu.vendeli.tgbot.utils.onCallbackQuery
import kotlinx.coroutines.Dispatchers
import me.yailya.step_ahead_bot.commands.handleFAQCommand
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

    val bot = TelegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

    bot.handleUpdates {
        onCommand("/start") {
            handleStartCommand(user, bot)
        }

        onCommand("/faq") {
            handleFAQCommand(user, bot)
        }

        onCommand("/moderate") {
            handleModerateCommand(user, bot)
        }

        for (university in Universities) {
            onInput("university_create_review_step1_${university.key}") {
                handleCreateReviewStep1Input(
                    update.getUser(),
                    bot,
                    university.value,
                    update.text,
                    update.origin.message!!.messageId
                )
            }

            onInput("university_create_review_step2_${university.key}") {
                handleCreateReviewStep2Input(
                    update.getUser(),
                    bot,
                    university.value,
                    update.text,
                    update.origin.message!!.messageId
                )
            }

            onInput("university_create_review_step3_${university.key}") {
                handleCreateReviewStep3Input(
                    update.getUser(),
                    bot,
                    university.value,
                    update.text,
                    update.origin.message!!.messageId
                )
            }

            onInput("university_create_update_request_step1_${university.key}") {
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
            val data = update.callbackQuery.data!!

            when {
                data == "universities" -> {
                    handleUniversitiesCallback(update.user, bot)
                }

                data == "update_requests" -> {
                    handleUpdateRequestsCallback(update.user, bot)
                }

                data == "moderate_update_requests" -> {
                    handleModerateUpdateRequestCallback(update.user, bot, -1)
                }

                data.startsWith("university_") -> {
                    val university = Universities[data.split("_").last().toInt()]

                    when {
                        data == "university_${university.id}" -> {
                            handleUniversityCallback(update.user, bot, university)
                        }

                        else -> {
                            val dataSplit = data
                                .split("_")
                                .dropLast(1)

                            when (val dataWithoutUniversityId = dataSplit.joinToString("_")) {
                                "university_specialities" -> handleSpecialitiesCallback(update.user, bot, university)
                                "university_reviews" -> handleReviewsCallback(update.user, bot, university)
                                "university_create_review" -> handleCreateReviewCallback(update.user, bot, university)
                                "university_create_update_request" -> handleCreateUpdateRequestCallback(
                                    update.user,
                                    bot,
                                    university
                                )

                                else -> when {
                                    dataWithoutUniversityId.startsWith("university_create_review_step4_") -> {
                                        handleCreateReviewStep4Callback(
                                            update.user,
                                            bot,
                                            university,
                                            dataSplit.last().toInt()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                data.startsWith("moderate_update_request_") -> {
                    val updateRequestId = data.split("_").last().toInt()
                    val dataSplit = data
                        .split("_")
                        .dropLast(1)

                    when (dataSplit.joinToString("_")) {
                        "moderate_update_request" -> handleModerateUpdateRequestCallback(
                            update.user,
                            bot,
                            updateRequestId,
                            update.callbackQuery.message!!.messageId
                        )

                        "moderate_update_request_close_" -> handleModerateUpdateRequestCloseCallback(
                            update.user,
                            bot,
                            updateRequestId
                        )

                        "moderate_update_request_close_done_" -> handleModerateUpdateRequestCloseDoneCallback(
                            update.user,
                            bot,
                            updateRequestId
                        )
                    }
                }
            }

            answerCallbackQuery(update.callbackQuery.id).send(update.user, bot)
        }
    }
}

suspend fun <T> databaseQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }