package com.spotik.app.ui.theme

import android.content.Context
import android.content.SharedPreferences

object ThemePreferences {
    private const val PREFS_NAME = "spotik_theme_prefs"
    private const val KEY_THEME = "selected_theme"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_THEME, null)
        if (saved != null) {
            try {
                ThemeManager.setTheme(AppThemeType.valueOf(saved), persist = false)
            } catch (_: Exception) { }
        }
    }

    fun save(theme: AppThemeType) {
        prefs?.edit()?.putString(KEY_THEME, theme.name)?.apply()
    }
}

