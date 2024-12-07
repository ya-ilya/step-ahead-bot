package me.yailya.step_ahead_bot.university.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import me.yailya.step_ahead_bot.university.University

suspend fun BehaviourContext.universityHandleOlympiadCallback(
    query: DataCallbackQuery,
    olympiadId: Int,
    university: University
) {

}