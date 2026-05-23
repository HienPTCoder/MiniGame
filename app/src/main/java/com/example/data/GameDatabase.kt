package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_scores")
data class GameScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gridSize: Int,
    val themeName: String,
    val moves: Int,
    val timeSecs: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1, // We only store one active game
    val gridSize: Int,
    val themeName: String,
    val boardState: String, // Comma separated integers, e.g. "1,2,3,0,5,6,4,7,8"
    val moves: Int,
    val timeSecs: Int,
    val isCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface GameDao {
    @Query("SELECT * FROM game_scores ORDER BY timeSecs ASC, moves ASC")
    fun getAllScores(): Flow<List<GameScore>>

    @Query("SELECT * FROM game_scores WHERE gridSize = :gridSize ORDER BY timeSecs ASC, moves ASC LIMIT 5")
    fun getBestScores(gridSize: Int): Flow<List<GameScore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: GameScore)

    @Query("DELETE FROM game_scores")
    suspend fun deleteAllScores()

    @Query("SELECT * FROM game_state WHERE id = 1")
    suspend fun getActiveGameState(): GameState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(state: GameState)

    @Query("DELETE FROM game_state WHERE id = 1")
    suspend fun deleteActiveGameState()
}

@Database(entities = [GameScore::class, GameState::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
