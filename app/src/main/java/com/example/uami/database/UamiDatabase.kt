package com.example.uami.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [RecipeEntity::class, TranslationEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class UamiDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun translationDao(): TranslationDao

    companion object {
        @Volatile
        private var INSTANCE: UamiDatabase? = null

        fun getDatabase(context: Context): UamiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UamiDatabase::class.java,
                    "uami_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
