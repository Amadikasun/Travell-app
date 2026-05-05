package com.travellapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellapp.data.model.Attraction
import com.travellapp.data.model.AttractionType
import com.travellapp.data.model.Itinerary
import com.travellapp.data.model.Trip
import com.travellapp.data.repository.TripRepository
import com.travellapp.util.ItineraryPlanner
import com.travellapp.util.TakeoutPlace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    val trips: StateFlow<List<Trip>> = repository.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTripId = MutableStateFlow<Int?>(null)
    val currentTripId: StateFlow<Int?> = _currentTripId.asStateFlow()

    val currentTrip: StateFlow<Trip?> = _currentTripId
        .flatMapLatest { id ->
            if (id != null) flow { emit(repository.getTripById(id)) } else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val attractions: StateFlow<List<Attraction>> = _currentTripId
        .flatMapLatest { id ->
            if (id != null) repository.getAttractionsForTrip(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _itinerary = MutableStateFlow<Itinerary?>(null)
    val itinerary: StateFlow<Itinerary?> = _itinerary.asStateFlow()

    private val _saveResult = MutableSharedFlow<Int>()
    val saveResult: SharedFlow<Int> = _saveResult.asSharedFlow()

    fun selectTrip(tripId: Int) {
        _currentTripId.value = tripId
        _itinerary.value = null
    }

    fun saveTrip(trip: Trip, userEmail: String) {
        viewModelScope.launch {
            val tripWithUser = trip.copy(userEmail = userEmail)
            val id = if (trip.id == 0) {
                repository.insertTrip(tripWithUser)
            } else {
                repository.updateTrip(tripWithUser)
                trip.id
            }
            _currentTripId.value = id
            _saveResult.emit(id)
        }
    }

    fun deleteTrip(tripId: Int) {
        viewModelScope.launch { repository.deleteTripById(tripId) }
    }

    fun addAttraction(
        name: String,
        type: AttractionType,
        durationMinutes: Int,
        address: String,
        lat: Double = 0.0,
        lon: Double = 0.0,
        notes: String
    ) {
        val tripId = _currentTripId.value ?: return
        viewModelScope.launch {
            repository.insertAttraction(
                Attraction(
                    tripId = tripId,
                    name = name,
                    type = type,
                    estimatedDurationMinutes = durationMinutes,
                    address = address,
                    latitude = lat,
                    longitude = lon,
                    notes = notes
                )
            )
        }
    }

    fun importAttractions(places: List<TakeoutPlace>) {
        val tripId = _currentTripId.value ?: return
        viewModelScope.launch {
            places.forEach { place ->
                val type = if (AttractionType.isLargePark(place.name))
                    AttractionType.LARGE_PARK else AttractionType.REGULAR
                repository.insertAttraction(
                    Attraction(
                        tripId = tripId,
                        name = place.name,
                        type = type,
                        estimatedDurationMinutes = type.defaultDurationMinutes,
                        address = place.address,
                        latitude = place.lat,
                        longitude = place.lon
                    )
                )
            }
        }
    }

    fun updateAttraction(attraction: Attraction) {
        viewModelScope.launch { repository.updateAttraction(attraction) }
    }

    fun deleteAttraction(attractionId: Int) {
        viewModelScope.launch { repository.deleteAttractionById(attractionId) }
    }

    fun generateItinerary() {
        val trip = currentTrip.value ?: return
        viewModelScope.launch {
            val list = repository.getAttractionsForTripOnce(trip.id)
            _itinerary.value = ItineraryPlanner.plan(trip, list)
        }
    }

    fun clearItinerary() {
        _itinerary.value = null
    }
}
