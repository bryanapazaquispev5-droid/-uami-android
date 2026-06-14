package com.example.uami.recipes.models

data class ReviewModel(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val comment: String = "",
    val rating: Int = 5,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList()
)
