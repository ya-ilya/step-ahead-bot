package me.yailya.step_ahead_bot.university.ranking

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

object EduRankRanking {
    private const val URL = "https://edurank.org/geo/ru-moscow/"
    private val UNIVERSITY_BLOCK_REGEX = """<div class="block-cont pt-4 mb-4">(.*?)</div>""".toRegex()
    private val UNIVERSITY_NAME_REGEX = """</span>(.*?)</a>""".toRegex()
    private val UNIVERSITY_HREF_REGEX = """href="(.*?)"""".toRegex()
    private val UNIVERSITY_TBODY_REGEX = """<tbody class="row">(.*?)</tbody>""".toRegex()
    private val UNIVERSITY_RANK_PLACE_REGEX = """<span class="ranks__place">(.*?)</span>""".toRegex()

    class EduRankData(
        val rankingUrl: String,
        val rankInMoscow: Int,
        val rankInRussia: Int,
        val rankInEurope: Int,
        val rankInWorld: Int
    )

    val ranking = run {
        val httpClient = HttpClient.newHttpClient()

        val universitiesResponse = httpClient.send(
            HttpRequest.newBuilder().GET().uri(URI.create(URL)).build(),
            BodyHandlers.ofString()
        ).body().replace("\n", "")

        val result = mutableMapOf<String, EduRankData>()

        for (universityBlock in UNIVERSITY_BLOCK_REGEX.findAll(universitiesResponse).map { it.groupValues[1] }) {
            val name = UNIVERSITY_NAME_REGEX.find(universityBlock)!!.groupValues[1].trim()
            val href = UNIVERSITY_HREF_REGEX.find(universityBlock)!!.groupValues[1].trim()

            val universityResponse = httpClient.send(
                HttpRequest.newBuilder().GET().uri(URI.create(href)).build(),
                BodyHandlers.ofString()
            ).body().replace("\n", "")

            val universityTBody = UNIVERSITY_TBODY_REGEX.find(universityResponse)!!.groupValues[1].trim()
            val universityRankPlaces = UNIVERSITY_RANK_PLACE_REGEX.findAll(universityTBody).map { it.groupValues[1].trim().removeSuffix("%").toInt() }.toList()

            result[name] = EduRankData(
                href,
                universityRankPlaces[3],
                universityRankPlaces[2],
                universityRankPlaces[1],
                universityRankPlaces[0]
            )
        }

        result
    }
}