package me.yailya.step_ahead_bot.university.update_request

enum class UniversityUpdateRequestStatus(val text: String) {
    Open("Открыт"),
    ClosedAndDone("Закрыт (и выполнен)"),
    Closed("Закрыт")
}