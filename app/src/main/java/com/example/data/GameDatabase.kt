package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "level_scores")
data class LevelScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: Int,
    val score: Int,
    val stars: Int,
    val moves: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface GameDao {
    @Query("SELECT * FROM level_scores ORDER BY score DESC LIMIT 20")
    fun getTopScores(): Flow<List<LevelScore>>

    @Query("SELECT * FROM level_scores WHERE level = :level ORDER BY score DESC LIMIT 1")
    suspend fun getBestScoreForLevel(level: Int): LevelScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: LevelScore)

    @Query("DELETE FROM level_scores")
    suspend fun deleteAllScores()
}

@Database(entities = [LevelScore::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
