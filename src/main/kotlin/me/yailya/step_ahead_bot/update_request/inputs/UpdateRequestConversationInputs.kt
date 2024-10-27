package me.yailya.step_ahead_bot.update_request.inputs

class UpdateRequestConversationInputs(
    val inputs: MutableList<String> = mutableListOf(),
    val messages: MutableList<Long> = mutableListOf(),
    val originMessageId: Long
)