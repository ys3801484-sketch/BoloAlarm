package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BedtimeScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BedtimeDao {
    @Query("SELECT * FROM bedtime_schedule WHERE id = 1")
    fun getBedtimeScheduleFlow(): Flow<BedtimeScheduleEntity?>

    @Query("SELECT * FROM bedtime_schedule WHERE id = 1")
    suspend fun getBedtimeSchedule(): BedtimeScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: BedtimeScheduleEntity)

    @Update
    suspend fun update(schedule: BedtimeScheduleEntity)
}
