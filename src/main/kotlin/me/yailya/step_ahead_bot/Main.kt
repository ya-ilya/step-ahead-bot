@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.entities
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardRowBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.build
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.queries.callback.CallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.Dispatchers
import me.yailya.step_ahead_bot.answer.Answers
import me.yailya.step_ahead_bot.answer.handlers.handleAnswerCallback
import me.yailya.step_ahead_bot.answer.handlers.handleAnswerDeleteCallback
import me.yailya.step_ahead_bot.answer.handlers.handleAnswerQuestionCallback
import me.yailya.step_ahead_bot.assistant.Assistant
import me.yailya.step_ahead_bot.assistant.handlers.handleAssistantStop
import me.yailya.step_ahead_bot.bot_user.BotUserEntity
import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.commands.*
import me.yailya.step_ahead_bot.moderator.*
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.question.handlers.handleQuestionAcceptAnswerCallback
import me.yailya.step_ahead_bot.question.handlers.handleQuestionAnswerCallback
import me.yailya.step_ahead_bot.question.handlers.handleQuestionCallback
import me.yailya.step_ahead_bot.question.handlers.handleQuestionDeleteCallback
import me.yailya.step_ahead_bot.review.Reviews
import me.yailya.step_ahead_bot.review.handlers.handleReviewCallback
import me.yailya.step_ahead_bot.review.handlers.handleReviewDeleteCallback
import me.yailya.step_ahead_bot.teacher.TeacherAcademicTitle
import me.yailya.step_ahead_bot.teacher.TeacherEntity
import me.yailya.step_ahead_bot.teacher.Teachers
import me.yailya.step_ahead_bot.teacher.request.AddTeacherRequests
import me.yailya.step_ahead_bot.teacher.request.handlers.handleAddTeacherRequestCallback
import me.yailya.step_ahead_bot.teacher.request.handlers.handleAddTeacherRequestCloseCallback
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import me.yailya.step_ahead_bot.teacher.review.handlers.handleTeacherReviewCallback
import me.yailya.step_ahead_bot.teacher.review.handlers.handleTeacherReviewDeleteCallback
import me.yailya.step_ahead_bot.teacher.review.handlers.handleTeacherReviewTeacherCallback
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.university.UniversityEntity
import me.yailya.step_ahead_bot.university.handlers.*
import me.yailya.step_ahead_bot.university.ranking.EduRankRanking
import me.yailya.step_ahead_bot.update_request.UpdateRequests
import me.yailya.step_ahead_bot.update_request.handlers.handleUpdateRequestCallback
import me.yailya.step_ahead_bot.update_request.handlers.handleUpdateRequestCloseCallback
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
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

@Suppress("UNCHECKED_CAST")
suspend fun BehaviourContext.edit(
    query: CallbackQuery,
    entities: TextSourcesList? = null,
    linkPreviewOptions: LinkPreviewOptions? = null,
    replyMarkup: InlineKeyboardMarkup? = null
) {
    edit(
        message = query.message!! as ContentMessage<TextContent>,
        entities = entities ?: query.message!!.entities!!,
        linkPreviewOptions = linkPreviewOptions,
        replyMarkup = replyMarkup ?: query.message!!.reply_markup
    )
}

suspend fun BehaviourContext.replyOrEdit(
    conditionToReply: Boolean,
    query: CallbackQuery,
    entities: TextSourcesList,
    replyMarkup: InlineKeyboardMarkup? = null
) {
    if (conditionToReply) {
        reply(
            to = query,
            entities = entities,
            replyMarkup = replyMarkup
        )
    } else {
        edit(
            query = query,
            entities = entities,
            replyMarkup = replyMarkup
        )
    }
}

suspend fun BehaviourContext.editInlineButton(
    query: CallbackQuery,
    buttonFilter: (InlineKeyboardButton) -> Boolean,
    buttonTransformer: ((InlineKeyboardButton) -> InlineKeyboardButton)?
) {
    val keyboardBuilder = InlineKeyboardBuilder()

    for (row in query.message!!.reply_markup!!.keyboard) {
        val rowBuilder = InlineKeyboardRowBuilder()

        for (button in row) {
            if (buttonFilter(button)) {
                rowBuilder.add(buttonTransformer?.invoke(button) ?: continue)
            } else {
                rowBuilder.add(button)
            }
        }

        keyboardBuilder.add(rowBuilder.row)
    }

    edit(
        query = query,
        entities = query.message!!.entities!!,
        replyMarkup = keyboardBuilder.build()
    )
}

suspend fun main() {
    println("Loaded EduRank ranking. ${EduRankRanking.ranking.size} entries")

    try {
        Assistant
    } catch (ex: Exception) {
        println("Failed to load assistant: ${ex.message}")
    }

    val driverClassName = "com.mysql.cj.jdbc.Driver"
    val jdbcURL = "jdbc:mysql://database/database"
    val database = Database.connect(jdbcURL, driverClassName, "user", "user_secret")

    transaction(database) {
        SchemaUtils.create(
            Reviews,
            UpdateRequests,
            Questions,
            Answers,
            Universities,
            BotUsers,
            Teachers,
            TeacherReviews,
            AddTeacherRequests
        )
    }

    databaseQuery {
        if (BotUserEntity.all().empty()) {
            BotUserEntity.new {
                this.userId = 1005465506
                this.isModerator = true
                this.isAdministrator = true
            }
        }

        if (TeacherEntity.all().empty()) {
            TeacherEntity.new {
                this.university = UniversityEntity.findById(1)!!
                this.fullName = "Иванов Иван Иванович"
                this.experience = 5
                this.academicTitle = TeacherAcademicTitle.Docent
                this.specialities = listOf("Актер")
            }
        }
    }

    val bot = telegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

    bot.buildBehaviourWithLongPolling {
        onCommand("assistant") {
            this.handleAssistantCommand(it)
        }

        onCommand("start") {
            this.handleStartCommand(it)
        }

        onCommand("faq") {
            this.handleFaqCommand(it)
        }

        onCommand("moderate") {
            this.handleModerateCommand(it)
        }

        onCommand("universities") {
            this.handleUniversitiesCommand(it)
        }

        onDataCallbackQuery("answers") {
            this.handleAnswerCallback(it, -1)
        }

        onDataCallbackQuery("questions") {
            this.handleQuestionCallback(it, -1)
        }

        onDataCallbackQuery("teacher_reviews") {
            this.handleTeacherReviewCallback(it, -1)
        }

        onDataCallbackQuery("reviews") {
            this.handleReviewCallback(it, -1)
        }

        onDataCallbackQuery("add_teacher_requests") {
            this.handleAddTeacherRequestCallback(it, -1)
        }

        onDataCallbackQuery("moderate_add_teacher_requests") {
            this.handleModerateAddTeacherRequestCallback(it, -1)
        }

        onDataCallbackQuery("update_requests") {
            this.handleUpdateRequestCallback(it, -1)
        }

        onDataCallbackQuery("moderate_update_requests") {
            this.handleModerateUpdateRequestCallback(it, -1)
        }

        val addTeacherRequestRegex = "add_teacher_request(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(addTeacherRequestRegex) {
            val values = addTeacherRequestRegex.find(it.data)!!.groupValues
            val name = values[1]
            val addTeacherRequestId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleAddTeacherRequestCallback(it, addTeacherRequestId)
                }

                name == "close" -> {
                    this.handleAddTeacherRequestCloseCallback(it, addTeacherRequestId)
                }
            }
        }

        val updateRequestRegex = "update_request(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(updateRequestRegex) {
            val values = updateRequestRegex.find(it.data)!!.groupValues
            val name = values[1]
            val updateRequestId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleUpdateRequestCallback(it, updateRequestId)
                }

                name == "close" -> {
                    this.handleUpdateRequestCloseCallback(it, updateRequestId)
                }
            }
        }

        val teacherReviewRegex = "teacher_review(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(teacherReviewRegex) {
            val values = teacherReviewRegex.find(it.data)!!.groupValues
            val name = values[1]
            val teacherReviewId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleTeacherReviewCallback(it, teacherReviewId)
                }

                name == "teacher" -> {
                    this.handleTeacherReviewTeacherCallback(it, teacherReviewId)
                }

                name == "delete" -> {
                    this.handleTeacherReviewDeleteCallback(it, teacherReviewId)
                }
            }
        }

        val answerRegex = "answer(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(answerRegex) {
            val values = answerRegex.find(it.data)!!.groupValues
            val name = values[1]
            val answerId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleAnswerCallback(it, answerId)
                }

                name == "question" -> {
                    this.handleAnswerQuestionCallback(it, answerId)
                }

                name == "delete" -> {
                    this.handleAnswerDeleteCallback(it, answerId)
                }
            }
        }

        val questionRegex = "question(?:_(.*))?_([^_]*)".toRegex()
        val questionAcceptAnswerRegex = "accept_answer_(.*?)$".toRegex()
        val questionAnswerRegex = "answer_(.*?)$".toRegex()

        onDataCallbackQuery(questionRegex) {
            val values = questionRegex.find(it.data)!!.groupValues
            val name = values[1]
            val questionId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleQuestionCallback(it, questionId)
                }

                name == "delete" -> {
                    this.handleQuestionDeleteCallback(it, questionId)
                }

                name == "answers" -> {
                    this.handleQuestionAnswerCallback(
                        it,
                        -1,
                        questionId
                    )
                }

                questionAcceptAnswerRegex.matches(name) -> {
                    this.handleQuestionAcceptAnswerCallback(
                        it,
                        questionAnswerRegex.find(name)!!.groupValues[1].toInt(),
                        questionId
                    )
                }

                questionAnswerRegex.matches(name) -> {
                    this.handleQuestionAnswerCallback(
                        it,
                        questionAnswerRegex.find(name)!!.groupValues[1].toInt(),
                        questionId
                    )
                }
            }
        }

        val reviewRegex = "review(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(reviewRegex) {
            val values = reviewRegex.find(it.data)!!.groupValues
            val name = values[1]
            val reviewId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleReviewCallback(it, reviewId)
                }

                name == "delete" -> {
                    this.handleReviewDeleteCallback(it, reviewId)
                }
            }
        }

        val universityRegex = "university(?:_(.*))?_([^_]*)".toRegex()
        val universityTeacherRegex = "teacher_(.*?)$".toRegex()
        val universityReviewRegex = "review_(.*?)$".toRegex()
        val universityCreateTeacherReviewRegex = "create_teacher_review_(.*?)$".toRegex()
        val universityCreateQuestionAnswerRegex = "create_question_answer_(.*?)$".toRegex()
        val universityTeacherReviewsRegex = "teacher_reviews_(.*?)$".toRegex()
        val universityTeacherReviewRegex = "teacher_review_(.*?)_(.*?)$".toRegex()
        val universityQuestionAnswersRegex = "question_answers_(.*?)$".toRegex()
        val universityQuestionAnswerRegex = "question_answer_(.*?)_(.*?)$".toRegex()

        onDataCallbackQuery(universityRegex) {
            val values = universityRegex.find(it.data)!!.groupValues
            val name = values[1]
            val university = databaseQuery { UniversityEntity.findById(values[2].toInt())!!.toModel() }

            when {
                name.isEmpty() -> {
                    this.handleUniversityCallback(it, university)
                }

                name == "specialities" -> {
                    this.handleUniversitySpecialitiesCallback(it, university)
                }

                name == "questions" -> {
                    this.handleUniversityQuestionCallback(it, -1, university)
                }

                name == "teachers" -> {
                    this.handleUniversityTeacherCallback(it, -1, university)
                }

                name == "reviews" -> {
                    this.handleUniversityReviewCallback(it, -1, university)
                }

                universityTeacherReviewsRegex.matches(name) -> {
                    this.handleUniversityTeacherReviewCallback(
                        it,
                        -1,
                        universityTeacherReviewsRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                universityQuestionAnswersRegex.matches(name) -> {
                    this.handleUniversityQuestionAnswerCallback(
                        it,
                        -1,
                        universityQuestionAnswersRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                universityTeacherRegex.matches(name) -> {
                    this.handleUniversityTeacherCallback(
                        it,
                        universityTeacherRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                universityReviewRegex.matches(name) -> {
                    this.handleUniversityReviewCallback(
                        it,
                        universityReviewRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                universityTeacherReviewRegex.matches(name) -> {
                    val universityTeacherReviewValues = universityTeacherReviewRegex.find(name)!!.groupValues
                    this.handleUniversityTeacherReviewCallback(
                        it,
                        universityTeacherReviewValues[1].toInt(),
                        universityTeacherReviewValues[2].toInt(),
                        university
                    )
                }

                universityQuestionAnswerRegex.matches(name) -> {
                    val universityQuestionAnswerValues = universityQuestionAnswerRegex.find(name)!!.groupValues
                    this.handleUniversityQuestionAnswerCallback(
                        it,
                        universityQuestionAnswerValues[1].toInt(),
                        universityQuestionAnswerValues[2].toInt(),
                        university
                    )
                }

                universityCreateTeacherReviewRegex.matches(name) -> {
                    this.handleCreateTeacherReviewCallback(
                        it,
                        universityCreateTeacherReviewRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                name == "create_question" -> {
                    this.handleCreateQuestionCallback(it, university)
                }

                universityCreateQuestionAnswerRegex.matches(name) -> {
                    this.handleCreateQuestionAnswerCallback(
                        it,
                        universityCreateQuestionAnswerRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                name == "create_review" -> {
                    this.handleCreateReviewCallback(it, university)
                }

                name == "create_add_teacher_request" -> {
                    this.handleCreateAddTeacherRequestCallback(it, university)
                }

                name == "create_update_request" -> {
                    this.handleCreateUpdateRequestCallback(it, university)
                }
            }
        }

        val moderateAddTeacherRequestRegex = "moderate_add_teacher_request(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(moderateAddTeacherRequestRegex) {
            val values = moderateAddTeacherRequestRegex.find(it.data)!!.groupValues
            val name = values[1]
            val addTeacherRequestId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleModerateAddTeacherRequestCallback(it, addTeacherRequestId)
                }

                name == "close" -> {
                    this.handleModerateAddTeacherRequestCloseCallback(it, addTeacherRequestId)
                }

                name == "close_done" -> {
                    this.handleModerateAddTeacherRequestCloseDoneCallback(it, addTeacherRequestId)
                }
            }
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
        }

        val assistantStopRegex = "assistant_stop_(.*?)$".toRegex()

        onDataCallbackQuery(assistantStopRegex) {
            val values = assistantStopRegex.find(it.data)!!.groupValues
            val userId = values[1].toLong()

            handleAssistantStop(it, userId)
        }
    }.join()
}

suspend fun <T> databaseQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }