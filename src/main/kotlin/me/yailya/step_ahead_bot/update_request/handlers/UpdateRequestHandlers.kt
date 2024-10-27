package me.yailya.step_ahead_bot.update_request.handlers

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.User
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity

suspend fun handleUpdateRequestsCallback(
    user: User,
    bot: TelegramBot
) {
    val updateRequests = UpdateRequestEntity.getModelsByUserId(user.id)

    message {
        var result = if (updateRequests.isNotEmpty()) {
            "" - bold { "Список ваших запросов на изменении информации о ВУЗах:" }
        } else {
            "" - bold { "Вы еще не создавали запросы на изменение информации" }
        }

        for (updateRequest in updateRequests) {
            val university = Universities[updateRequest.universityId]

            result =
                result - "\n" - "[Запрос №${updateRequest.id}]\n- Университет: ${university.name}\n- Статус: ${updateRequest.status.text}" -
                        "\nИнформация, которую пользователь бы хотел поменять: " - blockquote { updateRequest.text }
        }

        result
    }.send(user, bot)
}