package com.travellapp.util

import com.travellapp.data.model.Attraction
import com.travellapp.data.model.DayPlan
import com.travellapp.data.model.Itinerary
import com.travellapp.data.model.ScheduledAttraction
import com.travellapp.data.model.Trip
import kotlin.math.*

object ItineraryPlanner {

    private const val DEFAULT_TRAVEL_MINUTES = 30
    private const val EARTH_RADIUS_KM = 6371.0
    private const val AVERAGE_SPEED_KMH = 35.0
    private const val MIN_TRAVEL_MINUTES = 10

    fun plan(trip: Trip, attractions: List<Attraction>): Itinerary {
        val remaining = attractions.sortedBy { it.priority }.toMutableList()
        val days = mutableListOf<DayPlan>()
        val endOfDayMinutes = trip.endOfDayHour * 60

        for (day in 1..trip.durationDays) {
            if (remaining.isEmpty()) {
                days.add(DayPlan(day, trip.departureHour * 60 + trip.departureMinute, emptyList()))
                continue
            }

            var currentMinutes = trip.departureHour * 60 + trip.departureMinute
            var prevLat = trip.hotelLatitude
            var prevLon = trip.hotelLongitude
            val scheduled = mutableListOf<ScheduledAttraction>()

            while (remaining.isNotEmpty()) {
                val candidate = findBestNext(
                    remaining, prevLat, prevLon, currentMinutes, endOfDayMinutes
                ) ?: break

                val (attraction, travelTime) = candidate
                val arrivalTime = currentMinutes + travelTime
                val departureTime = arrivalTime + attraction.estimatedDurationMinutes

                scheduled.add(
                    ScheduledAttraction(
                        attraction = attraction,
                        arrivalTimeMinutes = arrivalTime,
                        departureTimeMinutes = departureTime,
                        travelTimeFromPreviousMinutes = travelTime
                    )
                )

                currentMinutes = departureTime
                prevLat = attraction.latitude
                prevLon = attraction.longitude
                remaining.remove(attraction)
            }

            days.add(
                DayPlan(
                    dayNumber = day,
                    departureTimeMinutes = trip.departureHour * 60 + trip.departureMinute,
                    scheduledAttractions = scheduled
                )
            )
        }

        return Itinerary(trip = trip, days = days, unscheduledAttractions = remaining.toList())
    }

    private fun findBestNext(
        attractions: List<Attraction>,
        fromLat: Double,
        fromLon: Double,
        currentMinutes: Int,
        endOfDayMinutes: Int
    ): Pair<Attraction, Int>? {
        return attractions
            .map { it to estimateTravelMinutes(fromLat, fromLon, it.latitude, it.longitude) }
            .filter { (attraction, travel) ->
                currentMinutes + travel + attraction.estimatedDurationMinutes <= endOfDayMinutes
            }
            .minByOrNull { (_, travel) -> travel }
    }

    private fun estimateTravelMinutes(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Int {
        val hasCoords = !(lat1 == 0.0 && lon1 == 0.0) && !(lat2 == 0.0 && lon2 == 0.0)
        if (!hasCoords) return DEFAULT_TRAVEL_MINUTES
        val distKm = haversine(lat1, lon1, lat2, lon2)
        return ((distKm / AVERAGE_SPEED_KMH) * 60).toInt().coerceAtLeast(MIN_TRAVEL_MINUTES)
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }
}
