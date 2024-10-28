package me.yailya.step_ahead_bot.update_request.handlers

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.textsources.blockquote
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import me.yailya.step_ahead_bot.reply
import me.yailya.step_ahead_bot.university.Universities
import me.yailya.step_ahead_bot.update_request.UpdateRequestEntity

suspend fun BehaviourContext.handleUpdateRequestsCallback(query: DataCallbackQuery) {
    val updateRequests = UpdateRequestEntity.getModelsByUserId(query.user.id.chatId.long)

    reply(
        to = query,
        buildEntities {
            if (updateRequests.isNotEmpty()) {
                +bold("Список ваших запросов на изменении информации о ВУЗах:")
            } else {
                +bold("Вы еще не создавали запросы на изменение информации")
            }

            for (updateRequest in updateRequests) {
                val university = Universities[updateRequest.universityId]

                +"\n" + "[Запрос №${updateRequest.id}]\n- Университет: ${university.name}\n- Статус: ${updateRequest.status.text}"

                if (updateRequest.moderatorId != null && updateRequest.commentFromModeration != null) {
                    +"\n- Комментарий от модератора #${updateRequest.moderatorId}: " + blockquote(updateRequest.commentFromModeration)
                }

                +"\nИнформация, которую пользователь бы хотел поменять: " + blockquote(updateRequest.text)
            }
        }
    )
}