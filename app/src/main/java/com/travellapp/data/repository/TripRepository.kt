package com.travellapp.data.repository

import com.travellapp.data.db.AttractionDao
import com.travellapp.data.db.TripDao
import com.travellapp.data.model.Attraction
import com.travellapp.data.model.Trip
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao,
    private val attractionDao: AttractionDao
) {
    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()

    suspend fun getTripById(id: Int): Trip? = tripDao.getTripById(id)

    suspend fun insertTrip(trip: Trip): Int = tripDao.insertTrip(trip).toInt()

    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)

    suspend fun deleteTripById(id: Int) = tripDao.deleteTripById(id)

    fun getAttractionsForTrip(tripId: Int): Flow<List<Attraction>> =
        attractionDao.getAttractionsForTrip(tripId)

    suspend fun getAttractionsForTripOnce(tripId: Int): List<Attraction> =
        attractionDao.getAttractionsForTripOnce(tripId)

    suspend fun insertAttraction(attraction: Attraction): Int =
        attractionDao.insertAttraction(attraction).toInt()

    suspend fun updateAttraction(attraction: Attraction) =
        attractionDao.updateAttraction(attraction)

    suspend fun deleteAttractionById(id: Int) =
        attractionDao.deleteAttractionById(id)
}
