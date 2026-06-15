package com.example.uami.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoriteManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("recipe_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun getFavoritesKey(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return if (uid != null) "favorites_list_$uid" else "favorites_list_anonymous"
    }

    fun saveFavorites(favorites: List<Int>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString(getFavoritesKey(), json).apply()
    }

    fun loadFavorites(): List<Int> {
        val json = prefs.getString(getFavoritesKey(), null) ?: return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type)
    }
}

