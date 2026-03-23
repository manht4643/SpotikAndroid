package com.spotik.app.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.spotik.app.data.City
import com.spotik.app.data.tatarCities

class RegistrationViewModel : ViewModel() {

    /* ── step 0..3 ── */
    var step by mutableIntStateOf(0)
        private set

    /* ── Step 0: City ── */
    var searchQuery by mutableStateOf("")
    var selectedCity by mutableStateOf<City?>(null)
        private set
    var cityConfirmed by mutableStateOf(false)
        private set

    val filteredCities: List<City>
        get() {
            if (searchQuery.isBlank()) return tatarCities
            val q = searchQuery.lowercase()
            return tatarCities.filter { it.name.lowercase().contains(q) }
        }

    fun selectCity(city: City) {
        selectedCity = city
        searchQuery = city.name
        cityConfirmed = true
    }

    /* ── Step 1: Name ── */
    var name by mutableStateOf("")

    /* ── Step 2: Age ── */
    var age by mutableIntStateOf(18)

    /* ── Step 3: Photo ── */
    var avatarUri by mutableStateOf<Uri?>(null)
        private set

    fun setAvatar(uri: Uri?) { avatarUri = uri }

    /* ── Navigation ── */
    fun nextStep() {
        if (step < 3) step++
    }

    fun prevStep() {
        if (step > 0) step--
    }

    val canProceed: Boolean
        get() = when (step) {
            0 -> cityConfirmed
            1 -> name.isNotBlank()
            2 -> age in 14..99
            3 -> true          // photo optional
            else -> false
        }

    val isLastStep get() = step == 3
}

