package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    fun getBestScores(gridSize: Int): Flow<List<GameScore>> = gameDao.getBestScores(gridSize)
    
    fun getAllScores(): Flow<List<GameScore>> = gameDao.getAllScores()

    suspend fun insertScore(score: GameScore) = gameDao.insertScore(score)

    suspend fun clearHistory() = gameDao.deleteAllScores()

    suspend fun getActiveGame(): GameState? = gameDao.getActiveGameState()

    suspend fun saveActiveGame(state: GameState) = gameDao.saveGameState(state)

    suspend fun deleteActiveGame() = gameDao.deleteActiveGameState()
}
