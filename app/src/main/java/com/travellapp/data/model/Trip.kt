package com.travellapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val destination: String = "",
    val durationDays: Int,
    val breakfastHour: Int,
    val breakfastMinute: Int = 0,
    val departureHour: Int,
    val departureMinute: Int = 0,
    val endOfDayHour: Int = 22,
    val hotelLatitude: Double = 0.0,
    val hotelLongitude: Double = 0.0,
    val hotelAddress: String = "",
    val userEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
