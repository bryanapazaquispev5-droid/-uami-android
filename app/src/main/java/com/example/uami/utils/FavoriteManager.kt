package com.example.uami.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoriteManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("recipe_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveFavorites(favorites: List<Int>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString("favorites_list", json).apply()
    }

    fun loadFavorites(): List<Int> {
        val json = prefs.getString("favorites_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type)
    }
}
