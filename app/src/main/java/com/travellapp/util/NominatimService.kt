package com.travellapp.util

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class NominatimResult(
    val displayName: String,
    val shortName: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val type: String
)

object NominatimService {

    private const val SEARCH_URL = "https://nominatim.openstreetmap.org/search"
    private const val USER_AGENT = "TravellApp/1.0 (Android travel planner)"
    private const val TIMEOUT_MS = 6000

    suspend fun search(query: String): List<NominatimResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("$SEARCH_URL?q=$encoded&format=json&limit=6&addressdetails=1&accept-language=cs,en")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        try {
            val body = conn.inputStream.bufferedReader().readText()
            parseResults(body)
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    private fun parseResults(json: String): List<NominatimResult> {
        val array = JsonParser.parseString(json).asJsonArray
        return array.mapNotNull { elem ->
            runCatching {
                val obj = elem.asJsonObject
                val displayName = obj.get("display_name").asString
                val lat = obj.get("lat").asString.toDouble()
                val lon = obj.get("lon").asString.toDouble()
                val type = obj.get("type")?.asString ?: ""

                val addr = obj.getAsJsonObject("address")
                val shortName = addr?.let {
                    it.get("tourism")?.asString
                        ?: it.get("amenity")?.asString
                        ?: it.get("leisure")?.asString
                        ?: it.get("historic")?.asString
                        ?: it.get("attraction")?.asString
                        ?: it.get("building")?.asString
                } ?: displayName.substringBefore(",").trim()

                val addressParts = buildList {
                    addr?.get("road")?.asString?.let { add(it) }
                    addr?.get("city")?.asString?.let { add(it) }
                        ?: addr?.get("town")?.asString?.let { add(it) }
                        ?: addr?.get("village")?.asString?.let { add(it) }
                    addr?.get("country")?.asString?.let { add(it) }
                }

                NominatimResult(
                    displayName = displayName,
                    shortName = shortName,
                    address = addressParts.joinToString(", "),
                    lat = lat,
                    lon = lon,
                    type = type
                )
            }.getOrNull()
        }
    }
}
