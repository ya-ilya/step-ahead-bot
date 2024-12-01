package me.yailya.step_ahead_bot.teacher.request

enum class AddTeacherRequestStatus(val text: String) {
    Open("Открыт"),
    ClosedAndDone("Закрыт (и выполнен)"),
    Closed("Закрыт")
}