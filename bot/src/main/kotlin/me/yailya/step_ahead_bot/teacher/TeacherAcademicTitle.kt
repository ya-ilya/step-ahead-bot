package me.yailya.step_ahead_bot.teacher

enum class TeacherAcademicTitle(val text: String) {
    Assistant("Ассистент"),
    Docent("Доцент"),
    Professor("Профессор"),
    HeadOfTheDepartment("Заведующий кафедрой");

    override fun toString(): String {
        return text
    }
}