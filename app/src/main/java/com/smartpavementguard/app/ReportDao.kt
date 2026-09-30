package com.smartpavementguard.app

import androidx.room.*

@Dao
interface ReportDao {

    @Insert
    suspend fun insert(report: LocalReport)

    @Query("SELECT * FROM local_reports WHERE synced = 0")
    suspend fun getPendingReports(): List<LocalReport>

    @Query("UPDATE local_reports SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
}