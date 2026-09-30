package com.smartpavementguard.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_reports")
data class LocalReport(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val impact: Float,
    val speed: Int,
    val priority: Int,
    val status: String,

    val synced: Boolean = false
)