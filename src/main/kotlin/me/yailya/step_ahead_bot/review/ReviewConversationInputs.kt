package me.yailya.step_ahead_bot.review

class ReviewConversationInputs(
    val inputs: MutableList<String> = mutableListOf(),
    val messages: MutableList<Long> = mutableListOf(),
    val originMessageId: Long
)