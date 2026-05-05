package com.travellapp.data.db

import androidx.room.*
import com.travellapp.data.model.Attraction
import kotlinx.coroutines.flow.Flow

@Dao
interface AttractionDao {
    @Query("SELECT * FROM attractions WHERE tripId = :tripId ORDER BY priority ASC, id ASC")
    fun getAttractionsForTrip(tripId: Int): Flow<List<Attraction>>

    @Query("SELECT * FROM attractions WHERE tripId = :tripId ORDER BY priority ASC, id ASC")
    suspend fun getAttractionsForTripOnce(tripId: Int): List<Attraction>

    @Query("SELECT * FROM attractions WHERE id = :id")
    suspend fun getAttractionById(id: Int): Attraction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttraction(attraction: Attraction): Long

    @Update
    suspend fun updateAttraction(attraction: Attraction)

    @Delete
    suspend fun deleteAttraction(attraction: Attraction)

    @Query("DELETE FROM attractions WHERE id = :id")
    suspend fun deleteAttractionById(id: Int)

    @Query("DELETE FROM attractions WHERE tripId = :tripId")
    suspend fun deleteAllForTrip(tripId: Int)
}
