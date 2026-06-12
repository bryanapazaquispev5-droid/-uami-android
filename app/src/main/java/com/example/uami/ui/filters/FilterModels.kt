package com.example.uami.ui.filters

data class FilterState(
    val searchQuery: String = "",
    val selectedCuisine: String = "All",
    val selectedDifficulty: String = "All",
    val selectedMealType: String = "All",
    val sortOrder: String = "Default"
)

enum class SortOption(val id: String, val labelEs: String, val labelEn: String) {
    DEFAULT("Default", "Por defecto", "Default"),
    NAME_AZ("A-Z", "Nombre A-Z", "Name A-Z"),
    NAME_ZA("Z-A", "Nombre Z-A", "Name Z-A"),
    RATING("Rating", "Mejor valorados", "Top Rated"),
    TIME("Time", "Menor tiempo", "Fastest"),
    INGREDIENTS("Ingredients", "Más ingredientes", "Most Ingredients")
}
