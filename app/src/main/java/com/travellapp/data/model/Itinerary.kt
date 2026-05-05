package com.travellapp.data.model

data class ScheduledAttraction(
    val attraction: Attraction,
    val arrivalTimeMinutes: Int,
    val departureTimeMinutes: Int,
    val travelTimeFromPreviousMinutes: Int
)

data class DayPlan(
    val dayNumber: Int,
    val departureTimeMinutes: Int,
    val scheduledAttractions: List<ScheduledAttraction>,
    val totalTravelMinutes: Int = scheduledAttractions.sumOf { it.travelTimeFromPreviousMinutes },
    val totalAttractionMinutes: Int = scheduledAttractions.sumOf {
        it.departureTimeMinutes - it.arrivalTimeMinutes
    }
)

data class Itinerary(
    val trip: Trip,
    val days: List<DayPlan>,
    val unscheduledAttractions: List<Attraction> = emptyList()
)
