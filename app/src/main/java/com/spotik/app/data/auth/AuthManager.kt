package com.spotik.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Manages JWT-token storage and authentication state.
 * Must call [init] before any usage (typically in Application.onCreate or Activity.onCreate).
 */
object AuthManager {
    private const val PREFS = "spotik_auth"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_NAME = "user_name"
    private const val KEY_HAS_PROFILE = "has_profile"

    private lateinit var prefs: SharedPreferences

    /** Observable auth state — Compose screens can read this directly */
    var isAuthenticated by mutableStateOf(false)
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        isAuthenticated = !token.isNullOrBlank()
    }

    var token: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_TOKEN, null) else null
        set(value) {
            prefs.edit { putString(KEY_TOKEN, value) }
            isAuthenticated = !value.isNullOrBlank()
        }

    var userId: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_USER_ID, null) else null
        set(value) = prefs.edit { putString(KEY_USER_ID, value) }

    var email: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_EMAIL, null) else null
        set(value) = prefs.edit { putString(KEY_EMAIL, value) }

    var userName: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_NAME, null) else null
        set(value) = prefs.edit { putString(KEY_NAME, value) }

    var hasProfile: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_HAS_PROFILE, false) else false
        set(value) = prefs.edit { putBoolean(KEY_HAS_PROFILE, value) }

    fun saveSession(token: String, userId: String, email: String?) {
        this.token = token
        this.userId = userId
        this.email = email
    }

    fun logout() {
        prefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_NAME)
            remove(KEY_HAS_PROFILE)
        }
        isAuthenticated = false
    }
}

