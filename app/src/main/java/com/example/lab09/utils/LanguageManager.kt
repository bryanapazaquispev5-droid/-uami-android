package com.example.lab09.utils

import android.content.Context
import android.content.SharedPreferences

object LanguageManager {
    private const val PREF_NAME = "language_prefs"
    private const val KEY_LANG = "current_language"
    
    private const val KEY_FIRST_RUN = "is_first_run"
    
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstRun(): Boolean {
        return prefs.getBoolean(KEY_FIRST_RUN, true)
    }

    fun setFirstRunCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANG, "") ?: ""
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
    }
}
