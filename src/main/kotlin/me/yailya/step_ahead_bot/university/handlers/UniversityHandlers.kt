package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.LinkPreviewOptions
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.message.textsources.italic
import dev.inmo.tgbotapi.types.message.textsources.link
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.EntitiesBuilder
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.expandableBlockquote
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.answer.Answer
import me.yailya.step_ahead_bot.answer.AnswerEntity
import me.yailya.step_ahead_bot.answer.Answers
import me.yailya.step_ahead_bot.databaseQuery
import me.yailya.step_ahead_bot.question.Question
import me.yailya.step_ahead_bot.question.QuestionEntity
import me.yailya.step_ahead_bot.question.Questions
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.replyOrEdit
import me.yailya.step_ahead_bot.review.Review
import me.yailya.step_ahead_bot.review.ReviewEntity
import me.yailya.step_ahead_bot.review.Reviews
import me.yailya.step_ahead_bot.teacher.Teacher
import me.yailya.step_ahead_bot.teacher.TeacherEntity
import me.yailya.step_ahead_bot.teacher.Teachers
import me.yailya.step_ahead_bot.teacher.review.TeacherReview
import me.yailya.step_ahead_bot.teacher.review.TeacherReviewEntity
import me.yailya.step_ahead_bot.teacher.review.TeacherReviews
import me.yailya.step_ahead_bot.university.University
import me.yailya.step_ahead_bot.university.ranking.EduRankRanking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

suspend fun BehaviourContext.handleUniversityCallback(query: DataCallbackQuery, university: University) {
    val universityRankData = EduRankRanking.ranking[university.nameEn]!!

    reply(
        to = query,
        buildEntities {
            var extraPointsText = EntitiesBuilder()

            for (extraPoints in university.extraPoints) {
                extraPointsText += "\n- ${extraPoints.value} балла за ${extraPoints.key.text}"
            }

            +bold(university.name) +
                    "\n" + expandableBlockquote(university.description) +
                    "\n✔\uFE0F Возможности: " + italic(university.facilities.joinToString { it.text }) +
                    "\n\uD83C\uDF10 Веб-сайт ВУЗа: " + link(university.website) +
                    "\n\uD83D\uDCCC Локация: " + link("Яндекс.Карты", university.location) +
                    "\n" + bold("Информация о бюджетном образовании") +
                    "\n" + "- Бюджетных мест: ${university.budgetInfo.placesCount}" +
                    "\n" + "- Средний балл на бюджет: ${university.budgetInfo.averagePoints}" +
                    "\n" + "- Самый низкий балл на бюджет: ${university.budgetInfo.minimalPoints}" +
                    "\n" + bold("Информация о платном образовании") +
                    "\n" + "- Платных мест: ${university.paidInfo.placesCount}" +
                    "\n" + "- Средняя стоимость: ${university.paidInfo.averagePrice}" +
                    "\n" + "- Самая низкая стоимость: ${university.paidInfo.minimalPrice}" +
                    "\n" + bold("В цифрах") +
                    "\n" + "- Ранг в Москве: " + link(
                "#${universityRankData.rankInMoscow}",
                universityRankData.rankingUrl
            ) +
                    "\n" + "- Студентов: ${university.inNumbers.studentsCount}" +
                    "\n" + "- Преподователей: ${university.inNumbers.professorsCount}" +
                    "\n" + "- Год основания: ${university.inNumbers.yearOfFoundation}" +
                    "\n" + bold("Дополнительные баллы для поступления") + extraPointsText +
                    "\n" + bold("Списки поступающих в ${university.shortName}") +
                    "\n" + "Списки поступающих доступны на этом сайте: " + link(university.listOfApplicants) +
                    "\n" + bold("Контакты ${university.shortName}") +
                    "\n" + "- Номер телефона: ${university.contacts.phone}" +
                    "\n" + "- Электронная почта: ${university.contacts.email}" +
                    "\n" + bold("Социальные сети: ") + link(
                "ВКонтакте",
                university.socialNetworks.vk
            ) + ", " + link("Telegram", university.socialNetworks.tg)
        },
        linkPreviewOptions = LinkPreviewOptions.Disabled,
        replyMarkup = inlineKeyboard {
            row {
                dataButton("\uD83C\uDF93 Специальности", "university_specialities_${university.id}")
            }

            row {
                dataButton("\uD83D\uDC69\u200D\uD83C\uDFEB Преподаватели", "university_teachers_${university.id}")
            }

            row {
                dataButton("❔ Вопросы", "university_questions_${university.id}")
                dataButton(
                    "✍\uD83C\uDFFB Задать вопрос",
                    "university_create_question_${university.id}"
                )
            }

            row {
                dataButton("⭐ Отзывы", "university_reviews_${university.id}")
                dataButton(
                    "✍\uD83C\uDFFB Создать отзыв",
                    "university_create_review_${university.id}"
                )
            }

            row {
                dataButton(
                    "✍\uD83C\uDFFB Создать запрос на добавление нового преподавателя",
                    "university_create_add_teacher_request_${university.id}"
                )
            }

            row {
                dataButton(
                    "✍\uD83C\uDFFB Создать запрос на изменение информации",
                    "university_create_update_request_${university.id}"
                )
            }
        }
    )

    answerCallbackQuery(query)
}

suspend fun BehaviourContext.handleUniversitySpecialitiesCallback(
    query: DataCallbackQuery,
    university: University
) {
    reply(
        to = query,
        buildEntities {
            +"" + bold("Специальности в ${university.shortName}") +
                    "\n" + "Доступные специальности:" +
                    "\n" + expandableBlockquote(university.specialities.joinToString("\n") { "- $it" })
        }
    )

    answerCallbackQuery(query)
}

private suspend fun teacherForKeyboard(
    id: Int,
    university: University
): Triple<Teacher?, Teacher, Teacher?> = databaseQuery {
    val condition = Teachers.university eq university.id
    val teachers = TeacherEntity.find(condition)

    if (teachers.empty()) {
        throw RuntimeException("❌ Преподователей в ${university.shortName} пока не добавлено")
    }

    val current = if (id == -1) {
        teachers.first()
    } else {
        TeacherEntity.findById(id) ?: throw RuntimeException("❌ Данный преподователь не найден")
    }

    val previous = TeacherEntity
        .find { condition and (Teachers.id less current.id) }
        .lastOrNull()
    val next = TeacherEntity
        .find { condition and (Teachers.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleUniversityTeacherCallback(
    query: DataCallbackQuery,
    teacherId: Int,
    university: University
) {
    val (previous, teacher, next) = try {
        teacherForKeyboard(teacherId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        teacherId == -1,
        query,
        buildEntities {
            +bold("${university.shortName} -> Преподователи -> ${teacher.fullName}") +
                    "\n- Опыт работы: ${teacher.experience}" +
                    "\n- Академическая должность: ${teacher.academicTitle}" +
                    "\n- Специальности: ${teacher.specialities.joinToString()}"
        },
        inlineKeyboard {
            row {
                dataButton(
                    "⭐ Отзывы о преподователе",
                    "university_teacher_reviews_${teacher.id}_${university.id}"
                )
            }

            row {
                dataButton(
                    "✍\uD83C\uDFFB Оставить отзыв о преподавателе",
                    "university_create_teacher_review_${teacher.id}_${university.id}"
                )
            }

            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_teacher_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_teacher_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

private suspend fun teacherReviewForKeyboard(
    id: Int,
    teacherId: Int,
    university: University
): Triple<TeacherReview?, TeacherReview, TeacherReview?> = databaseQuery {
    val condition = TeacherReviews.teacher eq teacherId
    val teacher = TeacherEntity.findById(teacherId) ?: throw RuntimeException("❌ Данный преподователь не найден")

    if (teacher.university.id.value != university.id) {
        throw RuntimeException("❌ Данный преподаватель работает в другом ВУЗе")
    }

    val teacherReviews = teacher.reviews

    if (teacherReviews.empty()) {
        throw RuntimeException("❌ Отзывов о данном преподавателе не найдено")
    }

    val current = if (id == -1) {
        teacherReviews.first()
    } else {
        TeacherReviewEntity.findById(id) ?: throw RuntimeException("❌ Данный отзыв о преподавателе не найден")
    }

    val previous = TeacherReviewEntity
        .find { condition and (TeacherReviews.id less current.id) }
        .lastOrNull()
    val next = TeacherReviewEntity
        .find { condition and (TeacherReviews.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleUniversityTeacherReviewCallback(
    query: DataCallbackQuery,
    teacherReviewId: Int,
    teacherId: Int,
    university: University
) {
    val (previous, teacherReview, next) = try {
        teacherReviewForKeyboard(teacherReviewId, teacherId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        teacherReviewId == -1,
        query,
        buildEntities {
            +bold("Отзыв о преподавателе #${teacherReview.id}. ${teacherReview.rating}/5") +
                    "\n" + blockquote(teacherReview.comment)
        },
        inlineKeyboard {
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_teacher_review_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_teacher_review_${next.id}_${university.id}")
                }
            }
        }
    )
}

private suspend fun questionForKeyboard(
    id: Int,
    university: University
): Triple<Question?, Question, Question?> = databaseQuery {
    val condition = Questions.university eq university.id
    val questions = QuestionEntity.find(condition)

    if (questions.empty()) {
        throw RuntimeException("❌ Вопросов о ${university.shortName} не найдено")
    }

    val current = if (id == -1) {
        questions.first()
    } else {
        QuestionEntity.findById(id) ?: throw RuntimeException("❌ Данного вопроса не существует")
    }

    val previous = QuestionEntity
        .find { condition and (Questions.id less current.id) }
        .lastOrNull()
    val next = QuestionEntity
        .find { condition and (Questions.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleUniversityQuestionCallback(
    query: DataCallbackQuery,
    questionId: Int,
    university: University
) {
    val (previous, question, next) = try {
        questionForKeyboard(questionId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        questionId == -1,
        query,
        buildEntities {
            +bold("Вопрос #${question.id}") +
                    "\n" + question.text
        },
        inlineKeyboard {
            row {
                dataButton(
                    "✍\uD83C\uDFFB Оставить ответ",
                    "university_create_question_answer_${question.id}_${university.id}"
                )
            }
            row {
                dataButton(
                    "\uD83D\uDE4B\uD83C\uDFFB\u200D♂\uFE0F Посмотреть ответы",
                    "university_question_answers_${question.id}_${university.id}"
                )
            }
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_question_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_question_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}

private suspend fun answerForKeyboard(
    id: Int,
    questionId: Int,
    university: University
): Triple<Answer?, Answer, Answer?> = databaseQuery {
    val condition = Answers.question eq questionId
    val question = QuestionEntity.findById(questionId) ?: throw RuntimeException("❌ Данный вопрос не существует")

    if (question.university.id.value != university.id) {
        throw RuntimeException("❌ Данный вопрос был задан о другом ВУЗе")
    }

    val answers = question.answers

    if (answers.empty()) {
        throw RuntimeException("❌ Ответов на этот вопрос нет")
    }

    val current = if (id == -1) {
        answers.first()
    } else {
        AnswerEntity.findById(id) ?: throw RuntimeException("❌ Данного ответа на вопрос не существует")
    }

    val previous = AnswerEntity
        .find { condition and (Answers.id less current.id) }
        .lastOrNull()
    val next = AnswerEntity
        .find { condition and (Answers.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleUniversityQuestionAnswerCallback(
    query: DataCallbackQuery,
    answerId: Int,
    questionId: Int,
    university: University
) {
    val (previous, answer, next) = try {
        answerForKeyboard(answerId, questionId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        answerId == -1,
        query,
        buildEntities {
            +bold("Ответ на вопрос о ${university.shortName} #${answer.id}") +
                    "\n" + answer.text
        },
        inlineKeyboard {
            row {
                if (previous != null) {
                    dataButton(
                        "⬅\uFE0F Предыдущий",
                        "university_question_answer_${previous.id}_${questionId}_${university.id}"
                    )
                }
                if (next != null) {
                    dataButton(
                        "Следующий ➡\uFE0F",
                        "university_question_answer_${next.id}_${questionId}_${university.id}"
                    )
                }
            }
        }
    )

    answerCallbackQuery(query)
}

private suspend fun reviewForKeyboard(
    id: Int,
    university: University
): Triple<Review?, Review, Review?> = databaseQuery {
    val condition = Reviews.university eq university.id
    val reviews = ReviewEntity.find(condition)

    if (reviews.empty()) {
        throw RuntimeException("❌ Отзывов о ${university.shortName} не найдено")
    }

    val current = if (id == -1) {
        reviews.first()
    } else {
        ReviewEntity.findById(id)
            ?: throw RuntimeException("❌ Данного отзыва не существует, либо же он был оставлен о другом ВУЗе")
    }

    val previous = ReviewEntity
        .find { condition and (Reviews.id less current.id) }
        .lastOrNull()
    val next = ReviewEntity
        .find { condition and (Reviews.id greater current.id) }
        .firstOrNull()

    return@databaseQuery Triple(
        previous?.toModel(),
        current.toModel(),
        next?.toModel()
    )
}

suspend fun BehaviourContext.handleUniversityReviewCallback(
    query: DataCallbackQuery,
    reviewId: Int,
    university: University
) {
    val (previous, review, next) = try {
        reviewForKeyboard(reviewId, university)
    } catch (ex: RuntimeException) {
        answerCallbackQuery(query, ex.message)
        return
    }

    replyOrEdit(
        reviewId == -1,
        query,
        buildEntities {
            +bold("Отзыв #${review.id}. ${review.rating}/5") +
                    "\n" + "Положительные стороны:" +
                    "\n" + blockquote(review.pros) +
                    "\n" + "Отрицательные стороны:" +
                    "\n" + blockquote(review.cons) +
                    "\n" + "Комментарий:" +
                    "\n" + blockquote(review.comment)
        },
        inlineKeyboard {
            row {
                if (previous != null) {
                    dataButton("⬅\uFE0F Предыдущий", "university_review_${previous.id}_${university.id}")
                }
                if (next != null) {
                    dataButton("Следующий ➡\uFE0F", "university_review_${next.id}_${university.id}")
                }
            }
        }
    )

    answerCallbackQuery(query)
}