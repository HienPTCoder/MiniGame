package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    fun getTopScores(): Flow<List<LevelScore>> = gameDao.getTopScores()
    suspend fun getBestScoreForLevel(level: Int): LevelScore? = gameDao.getBestScoreForLevel(level)
    suspend fun insertScore(score: LevelScore) = gameDao.insertScore(score)
    suspend fun clearHistory() = gameDao.deleteAllScores()
}
