package com.travellapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val KEY_EMAIL = stringPreferencesKey("user_email")
        val KEY_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val KEY_PHOTO_URL = stringPreferencesKey("user_photo_url")
    }

    val userEmail = dataStore.data
        .map { it[KEY_EMAIL] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val displayName = dataStore.data
        .map { it[KEY_DISPLAY_NAME] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isSignedIn = userEmail
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onSignInSuccess(email: String, displayName: String, photoUrl: String?) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_EMAIL] = email
                prefs[KEY_DISPLAY_NAME] = displayName
                if (photoUrl != null) prefs[KEY_PHOTO_URL] = photoUrl
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            dataStore.edit { it.clear() }
        }
    }
}
