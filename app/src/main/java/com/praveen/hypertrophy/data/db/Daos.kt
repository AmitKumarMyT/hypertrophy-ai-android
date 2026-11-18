package com.praveen.hypertrophy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY isCustom DESC, name ASC")
    fun getAllActive(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET archived = 1 WHERE id = :id")
    suspend fun archive(id: String)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM set_logs ORDER BY timestamp ASC")
    fun getAllSetLogs(): Flow<List<SetLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetLogEntity>)

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("SELECT * FROM time_logs ORDER BY timestamp ASC")
    fun getAllTimeLogs(): Flow<List<TimeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTime(time: TimeLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimes(times: List<TimeLogEntity>)

    @Query("DELETE FROM set_logs")
    suspend fun clearSetLogs()

    @Query("DELETE FROM time_logs")
    suspend fun clearTimeLogs()
}
