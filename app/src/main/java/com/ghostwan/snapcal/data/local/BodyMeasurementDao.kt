package com.ghostwan.snapcal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BodyMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurementEntity)

    @Query("SELECT * FROM body_measurements ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurements WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getHistory(startDate: String): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurements ORDER BY date ASC")
    suspend fun getAll(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurements WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): BodyMeasurementEntity?
}
