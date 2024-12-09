@file:OptIn(RiskFeature::class)

package me.yailya.step_ahead_bot

import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
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
    replyMarkup: InlineKeyboardMarkup? = null,
    linkPreviewOptions: LinkPreviewOptions? = null
) {
    if (conditionToReply) {
        reply(
            to = query,
            entities = entities,
            replyMarkup = replyMarkup,
            linkPreviewOptions = linkPreviewOptions
        )
    } else {
        edit(
            query = query,
            entities = entities,
            replyMarkup = replyMarkup,
            linkPreviewOptions = linkPreviewOptions
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