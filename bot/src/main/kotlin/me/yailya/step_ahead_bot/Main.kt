@file:Suppress("LocalVariableName")

package me.yailya.step_ahead_bot

import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import kotlinx.coroutines.Dispatchers
import me.yailya.step_ahead_bot.assistant.Assistant
import me.yailya.step_ahead_bot.assistant.handlers.handleAssistantStop
import me.yailya.step_ahead_bot.bot_user.BotUsers
import me.yailya.step_ahead_bot.commands.*
import me.yailya.step_ahead_bot.moderator.*
import me.yailya.step_ahead_bot.olympiad.Olympiads
import me.yailya.step_ahead_bot.olympiad.university_entry.OlympiadUniversityEntries
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.question.answer.QuestionAnswers
import me.yailya.step_ahead_bot.question.answer.handlers.handleAnswerCallback
import me.yailya.step_ahead_bot.question.answer.handlers.handleAnswerDeleteCallback
import me.yailya.step_ahead_bot.question.answer.handlers.handleAnswerQuestionCallback
import me.yailya.step_ahead_bot.question.handlers.handleQuestionAcceptAnswerCallback
import me.yailya.step_ahead_bot.question.handlers.handleQuestionAnswerCallback
import me.yailya.step_ahead_bot.question.handlers.handleQuestionCallback
import me.yailya.step_ahead_bot.question.handlers.handleQuestionDeleteCallback
import me.yailya.step_ahead_bot.teacher.Teachers
import me.yailya.step_ahead_bot.teacher.add_teacher_request.AddTeacherRequests
import me.yailya.step_ahead_bot.teacher.add_teacher_request.handlers.handleAddTeacherRequestCallback
import me.yailya.step_ahead_bot.teacher.add_teacher_request.handlers.handleAddTeacherRequestCloseCallback
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import me.yailya.step_ahead_bot.teacher.review.handlers.handleTeacherReviewCallback
import me.yailya.step_ahead_bot.teacher.review.handlers.handleTeacherReviewDeleteCallback
import me.yailya.step_ahead_bot.teacher.review.handlers.handleTeacherReviewTeacherCallback
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.university.UniversityEntity
import me.yailya.step_ahead_bot.university.handlers.*
import me.yailya.step_ahead_bot.university.ranking.EduRankRanking
import me.yailya.step_ahead_bot.university.review.UniversityReviews
import me.yailya.step_ahead_bot.university.review.handlers.handleUniversityReviewCallback
import me.yailya.step_ahead_bot.university.review.handlers.handleUniversityReviewDeleteCallback
import me.yailya.step_ahead_bot.university.update_request.UniversityUpdateRequests
import me.yailya.step_ahead_bot.university.update_request.handlers.handleUniversityUpdateRequestCallback
import me.yailya.step_ahead_bot.university.update_request.handlers.handleUniversityUpdateRequestCloseCallback
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun main() {
    println("Loaded EduRank ranking. ${EduRankRanking.ranking.size} entries")

    val database = Database.connect(
        "jdbc:mysql://database/database",
        "com.mysql.cj.jdbc.Driver",
        "user",
        "user_secret"
    )

    transaction(database) {
        SchemaUtils.create(
            UniversityReviews,
            UniversityUpdateRequests,
            Questions,
            QuestionAnswers,
            Universities,
            BotUsers,
            Teachers,
            TeacherReviews,
            AddTeacherRequests,
            Olympiads,
            OlympiadUniversityEntries
        )
    }

    try {
        Assistant.addUniversitiesData()
    } catch (ex: Exception) {
        println("Failed to load assistant: ${ex.message}")
    }

    val bot = telegramBot(System.getenv("TELEGRAM_BOT_TOKEN"))

    bot.buildBehaviourWithLongPolling {
        onCommand("assistant") {
            this.handleAssistantCommand(it)
        }

        onCommand("start") {
            this.handleStartCommand(it)
        }

        onCommand("moderate") {
            this.moderateHandleCommand(it)
        }

        onCommand("universities") {
            this.handleUniversitiesCommand(it)
        }

        onCommand("recommendations") {
            this.handleRecommendationsCommand(it)
        }

        onDataCallbackQuery("QuestionAnswers") {
            this.handleAnswerCallback(it, -1)
        }

        onDataCallbackQuery("Questions") {
            this.handleQuestionCallback(it, -1)
        }

        onDataCallbackQuery("TeacherReviews") {
            this.handleTeacherReviewCallback(it, -1)
        }

        onDataCallbackQuery("UniversityReviews") {
            this.handleUniversityReviewCallback(it, -1)
        }

        onDataCallbackQuery("AddTeacherRequests") {
            this.handleAddTeacherRequestCallback(it, -1)
        }

        onDataCallbackQuery("moderate_AddTeacherRequests") {
            this.moderateHandleAddTeacherRequestCallback(it, -1)
        }

        onDataCallbackQuery("UniversityUpdateRequests") {
            this.handleUniversityUpdateRequestCallback(it, -1)
        }

        onDataCallbackQuery("moderate_UniversityUpdateRequests") {
            this.moderateHandleUniversityUpdateRequestCallback(it, -1)
        }

        val addTeacherRequestRegex = "AddTeacherRequest(?:_(.*))?_([^_]*)".toRegex()

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

        val updateRequestRegex = "UniversityUpdateRequest(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(updateRequestRegex) {
            val values = updateRequestRegex.find(it.data)!!.groupValues
            val name = values[1]
            val updateRequestId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleUniversityUpdateRequestCallback(it, updateRequestId)
                }

                name == "close" -> {
                    this.handleUniversityUpdateRequestCloseCallback(it, updateRequestId)
                }
            }
        }

        val teacherReviewRegex = "TeacherReview(?:_(.*))?_([^_]*)".toRegex()

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

        val answerRegex = "QuestionAnswer(?:_(.*))?_([^_]*)".toRegex()

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

        val questionRegex = "Question(?:_(.*))?_([^_]*)".toRegex()
        val questionAcceptAnswerRegex = "accept_answer_(.*?)$".toRegex()
        val questionAnswerRegex = "QuestionAnswer_(.*?)$".toRegex()

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

                name == "QuestionAnswers" -> {
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

        val reviewRegex = "UniversityReview(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(reviewRegex) {
            val values = reviewRegex.find(it.data)!!.groupValues
            val name = values[1]
            val reviewId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.handleUniversityReviewCallback(it, reviewId)
                }

                name == "delete" -> {
                    this.handleUniversityReviewDeleteCallback(it, reviewId)
                }
            }
        }

        val universityRegex = "university(?:_(.*))?_([^_]*)".toRegex()
        val university_QuestionRegex = "Question_(.*?)$".toRegex()
        val university_TeacherRegex = "Teacher_(.*?)$".toRegex()
        val university_OlympiadRegex = "Olympiad_(.*?)$".toRegex()
        val university_UniversityReviewRegex = "UniversityReview_(.*?)$".toRegex()
        val university_CreateTeacherReviewRegex = "create_TeacherReview_(.*?)$".toRegex()
        val universityCreateQuestionAnswerRegex = "create_QuestionAnswer_(.*?)$".toRegex()
        val universityTeacherReviewsRegex = "TeacherReviews_(.*?)$".toRegex()
        val universityTeacherReviewRegex = "TeacherReview_(.*?)_(.*?)$".toRegex()
        val universityQuestionAnswersRegex = "QuestionAnswers_(.*?)$".toRegex()
        val universityQuestionAnswerRegex = "QuestionAnswer_(.*?)_(.*?)$".toRegex()

        onDataCallbackQuery(universityRegex) {
            val values = universityRegex.find(it.data)!!.groupValues
            val name = values[1]
            val university = databaseQuery { UniversityEntity.findById(values[2].toInt())!!.toModel() }

            when {
                name.isEmpty() -> {
                    this.handleUniversityCallback(it, university)
                }

                name == "specialities" -> {
                    this.universityHandleSpecialitiesCallback(it, university)
                }

                name == "Questions" -> {
                    this.universityHandleQuestionCallback(it, -1, university)
                }

                name == "Teachers" -> {
                    this.universityHandleTeacherCallback(it, -1, university)
                }

                name == "Olympiads" -> {
                    this.universityHandleOlympiadCallback(it, -1, university)
                }

                name == "UniversityReviews" -> {
                    this.universityHandleUniversityReviewCallback(it, -1, university)
                }

                universityTeacherReviewsRegex.matches(name) -> {
                    this.universityHandleTeacherReviewCallback(
                        it,
                        -1,
                        universityTeacherReviewsRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                universityQuestionAnswersRegex.matches(name) -> {
                    this.universityHandleQuestionAnswerCallback(
                        it,
                        -1,
                        universityQuestionAnswersRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                university_QuestionRegex.matches(name) -> {
                    this.universityHandleQuestionCallback(
                        it,
                        university_QuestionRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                university_TeacherRegex.matches(name) -> {
                    this.universityHandleTeacherCallback(
                        it,
                        university_TeacherRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                university_OlympiadRegex.matches(name) -> {
                    this.universityHandleOlympiadCallback(
                        it,
                        university_OlympiadRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                university_UniversityReviewRegex.matches(name) -> {
                    this.universityHandleUniversityReviewCallback(
                        it,
                        university_UniversityReviewRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                universityTeacherReviewRegex.matches(name) -> {
                    val universityTeacherReviewValues = universityTeacherReviewRegex.find(name)!!.groupValues
                    this.universityHandleTeacherReviewCallback(
                        it,
                        universityTeacherReviewValues[1].toInt(),
                        universityTeacherReviewValues[2].toInt(),
                        university
                    )
                }

                universityQuestionAnswerRegex.matches(name) -> {
                    val universityQuestionAnswerValues = universityQuestionAnswerRegex.find(name)!!.groupValues
                    this.universityHandleQuestionAnswerCallback(
                        it,
                        universityQuestionAnswerValues[1].toInt(),
                        universityQuestionAnswerValues[2].toInt(),
                        university
                    )
                }

                university_CreateTeacherReviewRegex.matches(name) -> {
                    this.universityHandleCreateTeacherReviewCallback(
                        it,
                        university_CreateTeacherReviewRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                name == "create_Question" -> {
                    this.universityHandleCreateQuestionCallback(it, university)
                }

                universityCreateQuestionAnswerRegex.matches(name) -> {
                    this.universityHandleCreateQuestionAnswerCallback(
                        it,
                        universityCreateQuestionAnswerRegex.find(name)!!.groupValues[1].toInt(),
                        university
                    )
                }

                name == "create_UniversityReview" -> {
                    this.universityHandleCreateUniversityReviewCallback(it, university)
                }

                name == "create_AddTeacherRequest" -> {
                    this.universityHandleCreateAddTeacherRequestCallback(it, university)
                }

                name == "create_UniversityUpdateRequest" -> {
                    this.universityHandleCreateUniversityUpdateRequestCallback(it, university)
                }
            }
        }

        val moderateAddTeacherRequestRegex = "moderate_AddTeacherRequest(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(moderateAddTeacherRequestRegex) {
            val values = moderateAddTeacherRequestRegex.find(it.data)!!.groupValues
            val name = values[1]
            val addTeacherRequestId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.moderateHandleAddTeacherRequestCallback(it, addTeacherRequestId)
                }

                name == "close" -> {
                    this.moderateHandleAddTeacherRequestCloseCallback(it, addTeacherRequestId)
                }

                name == "close_done" -> {
                    this.moderateHandleAddTeacherRequestCloseDoneCallback(it, addTeacherRequestId)
                }
            }
        }

        val moderateUniversityUpdateRequestRegex = "moderate_UniversityUpdateRequest(?:_(.*))?_([^_]*)".toRegex()

        onDataCallbackQuery(moderateUniversityUpdateRequestRegex) {
            val values = moderateUniversityUpdateRequestRegex.find(it.data)!!.groupValues
            val name = values[1]
            val updateRequestId = values[2].toInt()

            when {
                name.isEmpty() -> {
                    this.moderateHandleUniversityUpdateRequestCallback(it, updateRequestId)
                }

                name == "close" -> {
                    this.moderateHandleUniversityUpdateRequestCloseCallback(it, updateRequestId)
                }

                name == "close_done" -> {
                    this.moderateHandleUniversityUpdateRequestCloseDoneCallback(it, updateRequestId)
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