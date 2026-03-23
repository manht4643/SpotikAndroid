package com.spotik.app.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotik.app.data.api.RetrofitClient
import com.spotik.app.data.api.UpdateProfileRequest
import com.spotik.app.data.auth.AuthManager
import kotlinx.coroutines.launch

/**
 * ViewModel for the Profile screen.
 * Loads data from the server (with local fallback from AuthManager),
 * and syncs edits back.
 */
class ProfileViewModel : ViewModel() {

    var name by mutableStateOf("")
        private set
    var age by mutableIntStateOf(18)
        private set
    var bio by mutableStateOf("")
        private set
    var city by mutableStateOf("")
        private set
    var avatarUri by mutableStateOf<Uri?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    /* ── Editable copies (used inside Edit screen) ── */
    var editName by mutableStateOf("")
    var editAge by mutableIntStateOf(18)
    var editBio by mutableStateOf("")
    var editAvatarUri by mutableStateOf<Uri?>(null)

    init {
        loadProfile()
    }

    /** Start editing — copy current values into edit fields. */
    fun beginEdit() {
        editName = name
        editAge = age
        editBio = bio
        editAvatarUri = avatarUri
    }

    /** Apply edits to local state and sync to server. */
    fun applyEdit() {
        name = editName
        age = editAge
        bio = editBio
        avatarUri = editAvatarUri

        // Save locally for offline
        AuthManager.saveProfile(
            name = name,
            age = age,
            bio = bio,
            city = city,
            avatarUrl = avatarUri?.toString(),
        )

        // Push to server
        viewModelScope.launch {
            try {
                RetrofitClient.api.updateProfile(
                    UpdateProfileRequest(
                        name = name,
                        age = age,
                        bio = bio,
                        city = city,
                        avatarUrl = avatarUri?.toString(),
                    )
                )
            } catch (_: Exception) {
                // Will sync next time
            }
        }
    }

    fun setEditAvatar(uri: Uri?) {
        editAvatarUri = uri
    }

    /** Load profile: first from local prefs, then overwrite with server data. */
    private fun loadProfile() {
        // Immediate local data
        name = AuthManager.userName ?: ""
        age = AuthManager.userAge
        bio = AuthManager.userBio ?: ""
        city = AuthManager.userCity ?: ""
        val savedAvatar = AuthManager.userAvatar
        if (!savedAvatar.isNullOrBlank()) {
            avatarUri = Uri.parse(savedAvatar)
        }
        isLoading = false

        // Then fetch from server
        if (AuthManager.token != null) {
            viewModelScope.launch {
                try {
                    val profile = RetrofitClient.api.getProfile()
                    name = profile.name
                    age = profile.age
                    bio = profile.bio ?: ""
                    city = profile.city
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        avatarUri = Uri.parse(profile.avatarUrl)
                    }
                    // Update local cache
                    AuthManager.saveProfile(
                        name = profile.name,
                        age = profile.age,
                        bio = profile.bio,
                        city = profile.city,
                        avatarUrl = profile.avatarUrl,
                    )
                } catch (_: Exception) {
                    // Server unavailable — local data is already set
                }
            }
        }
    }

    /** Force reload (e.g. after login). */
    fun refresh() {
        loadProfile()
    }
}

