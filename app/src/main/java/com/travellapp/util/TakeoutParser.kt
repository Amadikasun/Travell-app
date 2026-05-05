package com.travellapp.util

import com.google.gson.JsonParser
import java.io.InputStream

data class TakeoutPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val googleMapsUrl: String = ""
)

object TakeoutParser {

    /**
     * Parses a Google Takeout "Saved Places.json" (GeoJSON FeatureCollection).
     * Returns a list of places grouped by their source list name inferred from
     * the file name, or a flat list if grouping is not available.
     */
    fun parse(inputStream: InputStream): List<TakeoutPlace> {
        val content = inputStream.bufferedReader().readText()
        return runCatching { parseGeoJson(content) }.getOrElse { emptyList() }
    }

    private fun parseGeoJson(json: String): List<TakeoutPlace> {
        val root = JsonParser.parseString(json).asJsonObject
        val features = root.getAsJsonArray("features") ?: return emptyList()

        return features.mapNotNull { elem ->
            runCatching {
                val obj = elem.asJsonObject
                val props = obj.getAsJsonObject("properties") ?: return@runCatching null
                val title = props.get("Title")?.asString ?: return@runCatching null

                // Coordinates from geometry (GeoJSON: [lon, lat])
                val geometry = obj.getAsJsonObject("geometry")
                val coords = geometry?.getAsJsonArray("coordinates")
                var lon = coords?.get(0)?.asDouble ?: 0.0
                var lat = coords?.get(1)?.asDouble ?: 0.0

                // Fallback: coordinates inside Location object
                val location = props.getAsJsonObject("Location")
                if (lat == 0.0 && lon == 0.0 && location != null) {
                    val geo = location.getAsJsonObject("Geo Coordinates")
                    lat = geo?.get("Latitude")?.asString?.toDoubleOrNull() ?: 0.0
                    lon = geo?.get("Longitude")?.asString?.toDoubleOrNull() ?: 0.0
                }

                val address = location?.get("Address")?.asString
                    ?: location?.get("Business Name")?.asString
                    ?: ""

                val mapsUrl = props.get("Google Maps URL")?.asString ?: ""

                TakeoutPlace(
                    name = title,
                    address = address,
                    lat = lat,
                    lon = lon,
                    googleMapsUrl = mapsUrl
                )
            }.getOrNull()
        }
    }
}
