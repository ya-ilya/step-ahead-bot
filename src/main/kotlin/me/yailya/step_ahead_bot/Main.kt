package me.yailya.step_ahead_bot

import eu.vendeli.tgbot.TelegramBot
import kotlinx.coroutines.Dispatchers
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

    bot.handleUpdates()
}

suspend fun <T> databaseQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }