package me.yailya.step_ahead_bot.commands

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.row
import me.yailya.step_ahead_bot.university.Universities

suspend fun BehaviourContext.handleUniversitiesCommand(message: TextMessage) {
    reply(
        to = message,
        text = "Список ВУЗов. Вы можете выбрать один из них, чтобы посмотреть подробную информацию",
        replyMarkup = inlineKeyboard {
            for (chunk in Universities.iterator().asSequence().toList().chunked(4)) {
                row {
                    for (university in chunk) {
                        dataButton("(${university.key}) ${university.value.shortName}", "university_${university.key}")
                    }
                }
            }
        }
    )
}