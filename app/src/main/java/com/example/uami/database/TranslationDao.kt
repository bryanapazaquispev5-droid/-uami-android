package com.example.uami.database

import androidx.room.*

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations")
    suspend fun getAllTranslations(): List<TranslationEntity>

    @Query("SELECT translatedText FROM translations WHERE originalText = :originalText LIMIT 1")
    suspend fun getTranslation(originalText: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(translation: TranslationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(translations: List<TranslationEntity>)

    @Query("DELETE FROM translations")
    suspend fun clearAll()
}
