package com.smartpavementguard.app

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
//Jared Ama a tiburcio y sus pañales
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {

        return INSTANCE ?: synchronized(this) {

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "aquaroad_database"
            ).build()

            INSTANCE = instance

            instance
        }
    }
}