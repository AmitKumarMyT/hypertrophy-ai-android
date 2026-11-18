package com.praveen.hypertrophy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.praveen.hypertrophy.EXERCISES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [ExerciseEntity::class, SetLogEntity::class, TimeLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hypertrophy.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default exercises on first DB creation
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).exerciseDao()
                            val baseline = EXERCISES.map { e ->
                                ExerciseEntity(
                                    id = e.id,
                                    name = e.name,
                                    primaryMuscles = e.primary.joinToString(","),
                                    secondaryMuscles = e.secondary.joinToString(","),
                                    loReps = e.lo,
                                    hiReps = e.hi,
                                    restSec = e.rest,
                                    isSec = e.isSec,
                                    loaded = e.loaded,
                                    ladderJson = Json.encodeToString(e.ladder),
                                    cue = e.cue,
                                    tipsDoJson = Json.encodeToString(e.tipsDo),
                                    tipsDontJson = Json.encodeToString(e.tipsDont),
                                    poseJson = "",
                                    videoPath = "",
                                    isCustom = false,
                                    archived = false
                                )
                            }
                            dao.insertAll(baseline)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
