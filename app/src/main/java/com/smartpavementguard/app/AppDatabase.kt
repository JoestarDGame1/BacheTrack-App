package com.smartpavementguard.app

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalReport::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
}