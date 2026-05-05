package com.travellapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.travellapp.data.model.Attraction
import com.travellapp.data.model.Trip

@Database(
    entities = [Trip::class, Attraction::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun attractionDao(): AttractionDao
}
