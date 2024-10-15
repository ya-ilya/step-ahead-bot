package me.yailya.step_ahead_bot.university

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

@Suppress("SameParameterValue")
object Universities {
    private val universities = run {
        val result = mutableMapOf<Int, UniversityModel>()

        for (resource in getResourceFiles("universities")) {
            if (resource.endsWith(".json")) {
                val university = Json.decodeFromString<UniversityModel>(getResourceAsStream("universities/${resource}").readAllBytes().toString(
                    Charset.defaultCharset()))

                result[university.id] = university
            }
        }

        result
    }

    operator fun get(universityId: Int): UniversityModel {
        return universities[universityId]!!
    }

    operator fun iterator() = universities.iterator()

    @Throws(IOException::class)
    private fun getResourceFiles(path: String): List<String> {
        val filenames: MutableList<String> = ArrayList()

        getResourceAsStream(path).use { `in` ->
            BufferedReader(InputStreamReader(`in`)).use { br ->
                var resource: String
                while ((br.readLine().also { resource = it }) != null) {
                    filenames.add(resource)
                }
            }
        }
        return filenames
    }

    private fun getResourceAsStream(resource: String): InputStream {
        val `in` = getContextClassLoader().getResourceAsStream(resource)

        return `in` ?: javaClass.getResourceAsStream(resource)!!
    }

    private fun getContextClassLoader(): ClassLoader {
        return Thread.currentThread().contextClassLoader
    }
}