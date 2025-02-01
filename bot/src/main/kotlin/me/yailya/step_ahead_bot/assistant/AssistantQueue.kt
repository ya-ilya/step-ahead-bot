package me.yailya.step_ahead_bot.assistant

class AssistantQueue(private val maxSize: Int) {
    private val queue = mutableSetOf<Long>()

    fun getUserPosition(id: Long): Int {
        return queue.indexOf(id) + 1
    }

    fun addUser(id: Long) {
        queue.add(id)
    }

    fun removeUser(id: Long) {
        queue.remove(id)
    }

    fun isFull(): Boolean {
        return queue.size == maxSize
    }
}