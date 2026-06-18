package com.example.uami.utils

import com.google.firebase.auth.FirebaseAuth

object AuthManager {
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }
}
