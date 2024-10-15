package me.yailya.step_ahead_bot.university

import kotlinx.serialization.Serializable

@Serializable
data class UniversityModel(
    val id: Int,
    val shortName: String,
    val name: String,
    val description: String,
    val website: String,
    val location: String,
    val facilities: List<UniversityFacility>,
    val budgetInfo: UniversityBudgetInfo,
    val paidInfo: UniversityPaidInfo,
    val extraPoints: Map<ExtraPointsReason, Int>,
    val inNumbers: UniversityInNumbers,
    val contacts: UniversityContacts,
    val listOfApplicants: String,
    val specialities: List<String>
) {
    @Serializable
    data class UniversityBudgetInfo(
        val year: Int,
        val placesCount: Int,
        val averagePoints: Float,
        val minimalPoints: Int
    )

    @Serializable
    data class UniversityPaidInfo(
        val year: Int,
        val placesCount: Int,
        val averagePrice: Float,
        val minimalPrice: Int
    )

    @Serializable
    data class UniversityInNumbers(
        val year: Int,
        val studentsCount: Int,
        val professorsCount: Int,
        val yearOfFoundation: Int
    )

    @Serializable
    data class UniversityContacts(
        val phone: String,
        val email: String
    )

    enum class UniversityFacility(val text: String) {
        Dormitory("Общежитие"),
        MilitaryCenter("Военный учебный центр"),
        BudgetPlaces("Бюджетные места"),
        Postponement("Отсрочка от армии")
    }

    enum class ExtraPointsReason(val text: String) {
        GoldGTOBadge("золотой значок ГТО"),
        CertificateWithDistinction("аттестат с отличием"),
        Portfolio("портфолио"),
        Essay("сочинение"),
        Soldering("прохождение воинской сложбы"),
        Volunteering("волонтерство")
    }
}