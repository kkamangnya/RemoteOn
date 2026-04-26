package com.kkamangnya.remoteon

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePrefs {
    private const val FILE_NAME = "remoteon_settings"
    private const val KEY_NIGHT_MODE = "night_mode"

    fun loadNightMode(context: Context): Int {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun saveNightMode(context: Context, nightMode: Int) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NIGHT_MODE, nightMode)
            .apply()
    }
}
