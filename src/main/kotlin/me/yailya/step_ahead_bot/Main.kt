package me.yailya.step_ahead_bot

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.answer.answerCallbackQuery
import eu.vendeli.tgbot.utils.onCallbackQuery
import kotlinx.coroutines.Dispatchers
import me.yailya.step_ahead_bot.commands.handleStartCommand
import me.yailya.step_ahead_bot.handlers.handleReviewsCallback
import me.yailya.step_ahead_bot.handlers.handleSpecialitiesCallback
import me.yailya.step_ahead_bot.handlers.handleUniversityCallback
import me.yailya.step_ahead_bot.university.Universities
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun main() {
    val driverClassName = "org.h2.Driver"
    val jdbcURL = "jdbc:h2:file:./database"
    val database = Database.connect(jdbcURL, driverClassName)

    transaction(database) {
        // SchemaUtils.create(...)

        addLogger(StdOutSqlLogger)
    }

    val bot = TelegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

    bot.handleUpdates {
        onCommand("/start") {
            handleStartCommand(user, bot)
        }

        onCallbackQuery {
            val university = Universities[update.callbackQuery.data!!.split("_").last().toInt()]

            when {
                update.callbackQuery.data!!.startsWith("university_") -> {
                    handleUniversityCallback(update.user, bot, university)
                }

                update.callbackQuery.data!!.contains("_") -> {
                    val callbackName = update.callbackQuery.data!!.split("_")[0]

                    when (callbackName) {
                        "specialities" -> handleSpecialitiesCallback(update.user, bot, university)
                        "reviews" -> handleReviewsCallback(update.user, bot, university)
                    }
                }
            }

            answerCallbackQuery(update.callbackQuery.id).send(update.user, bot)
        }
    }
}

suspend fun <T> databaseQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }