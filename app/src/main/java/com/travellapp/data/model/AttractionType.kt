package com.travellapp.data.model

enum class AttractionType(
    val label: String,
    val minDurationMinutes: Int,
    val maxDurationMinutes: Int,
    val defaultDurationMinutes: Int
) {
    REGULAR("Běžná atrakce", 90, 240, 120),
    LARGE_PARK("Velký park / Zábavní park", 180, 300, 240);

    companion object {
        fun isLargePark(name: String): Boolean {
            val lower = name.lowercase()
            return lower.contains("disneyland") ||
                lower.contains("disney") ||
                lower.contains("tropical island") ||
                lower.contains("tropical land") ||
                lower.contains("legoland") ||
                lower.contains("universal studios") ||
                lower.contains("zoo") ||
                lower.contains("safari") ||
                lower.contains("aquapark") ||
                lower.contains("waterpark")
        }
    }
}
