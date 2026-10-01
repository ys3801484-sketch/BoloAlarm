package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WorldClockCityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldClockDao {
    @Query("SELECT * FROM world_clock_cities ORDER BY orderIndex ASC, id ASC")
    fun getAllCities(): Flow<List<WorldClockCityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: WorldClockCityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cities: List<WorldClockCityEntity>)

    @Delete
    suspend fun deleteCity(city: WorldClockCityEntity)

    @Query("DELETE FROM world_clock_cities WHERE id = :id")
    suspend fun deleteCityById(id: Long)

    @Query("SELECT COUNT(*) FROM world_clock_cities")
    suspend fun getCityCount(): Int
}
