package com.travellapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attractions",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class Attraction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val name: String,
    val type: AttractionType,
    val estimatedDurationMinutes: Int,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val notes: String = "",
    val priority: Int = 0
)
