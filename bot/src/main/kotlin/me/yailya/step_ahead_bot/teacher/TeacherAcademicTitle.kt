package me.yailya.step_ahead_bot.teacher

enum class TeacherAcademicTitle(val text: String) {
    Docent("Доцент"),
    Professor("Профессор");

    override fun toString(): String {
        return text
    }
}