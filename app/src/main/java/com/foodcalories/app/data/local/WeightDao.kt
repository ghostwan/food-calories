package com.foodcalories.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weight: WeightEntity)

    @Query("SELECT * FROM weight_records ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): WeightEntity?

    @Query("SELECT * FROM weight_records ORDER BY date DESC LIMIT :days")
    suspend fun getHistory(days: Int): List<WeightEntity>

    @Query("SELECT * FROM weight_records ORDER BY date ASC")
    suspend fun getAll(): List<WeightEntity>

    @Query("SELECT * FROM weight_records WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): WeightEntity?
}
