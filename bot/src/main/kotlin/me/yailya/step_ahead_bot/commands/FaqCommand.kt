package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.content.TextMessage

suspend fun BehaviourContext.handleFaqCommand(message: TextMessage) {
    reply(
        to = message,
        text = "Вопрос/ответ:\n1. Как увидеть информацию о каком-либо ВУЗе: используйте команду /start, а затем используйте функциональную клавишу с кратким названием нужного ВУЗа."
    )
}