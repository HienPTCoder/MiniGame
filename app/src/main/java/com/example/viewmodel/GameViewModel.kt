package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PuzzleApplication
import com.example.data.GameScore
import com.example.data.GameState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ScreenState {
    Menu,
    Playing,
    Scores
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as PuzzleApplication).repository

    // Screen navigation state
    private val _screenState = MutableStateFlow(ScreenState.Menu)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // Game variables
    private val _gridSize = MutableStateFlow(4) // Default is 4x4
    val gridSize: StateFlow<Int> = _gridSize.asStateFlow()

    private val _themeName = MutableStateFlow("Classic") // "Classic", "Emoji", "Gradient"
    val themeName: StateFlow<String> = _themeName.asStateFlow()

    private val _tiles = MutableStateFlow<List<Int>>(emptyList())
    val tiles: StateFlow<List<Int>> = _tiles.asStateFlow()

    private val _moves = MutableStateFlow(0)
    val moves: StateFlow<Int> = _moves.asStateFlow()

    private val _timeSecs = MutableStateFlow(0)
    val timeSecs: StateFlow<Int> = _timeSecs.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _hasActiveSavedGame = MutableStateFlow(false)
    val hasActiveSavedGame: StateFlow<Boolean> = _hasActiveSavedGame.asStateFlow()

    // Settings
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _showIndicesInGradient = MutableStateFlow(false)
    val showIndicesInGradient: StateFlow<Boolean> = _showIndicesInGradient.asStateFlow()

    // Smart Hint index
    private val _hintTileValue = MutableStateFlow<Int?>(null)
    val hintTileValue: StateFlow<Int?> = _hintTileValue.asStateFlow()

    // High Scores flow
    val bestScores3x3 = repository.getBestScores(3).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bestScores4x4 = repository.getBestScores(4).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bestScores5x5 = repository.getBestScores(5).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Timer Job
    private var timerJob: Job? = null

    init {
        checkSavedGamePresence()
    }

    fun toggleSound() { _soundEnabled.value = !_soundEnabled.value }
    fun toggleHaptic() { _hapticEnabled.value = !_hapticEnabled.value }
    fun toggleShowIndicesInGradient() { _showIndicesInGradient.value = !_showIndicesInGradient.value }

    fun checkSavedGamePresence() {
        viewModelScope.launch {
            val savedGame = repository.getActiveGame()
            _hasActiveSavedGame.value = savedGame != null
        }
    }

    fun setScreenState(state: ScreenState) {
        _screenState.value = state
        if (state == ScreenState.Playing) {
            resumeTimer()
        } else {
            pauseTimerStateOnly()
        }
    }

    fun selectGridSize(size: Int) {
        _gridSize.value = size
    }

    fun selectTheme(theme: String) {
        _themeName.value = theme
    }

    // New Game Setup
    fun startNewGame() {
        val size = _gridSize.value
        val solvedBoard = (1 until size * size).toList() + listOf(0)
        
        // Let's scramble the board by making random legal moves to ensure 100% solvability
        val scrambledBoard = scramble(solvedBoard, size)
        
        _tiles.value = scrambledBoard
        _moves.value = 0
        _timeSecs.value = 0
        _isCompleted.value = false
        _isPaused.value = false
        _hintTileValue.value = null
        _screenState.value = ScreenState.Playing
        
        startTimer()
        saveCurrentGameState()
    }

    private fun scramble(solved: List<Int>, size: Int): List<Int> {
        val board = solved.toMutableList()
        var emptyIndex = board.indexOf(0)
        
        // Perform many valid swap moves
        val randomIterations = when (size) {
            3 -> 60
            4 -> 120
            else -> 180
        }
        
        var lastSwappedValue = -1
        for (i in 0 until randomIterations) {
            val possibleIndices = getAdjacentIndices(emptyIndex, size)
            // Filter out the inverse of the last move to make scramble more random
            val candidates = possibleIndices.filter { board[it] != lastSwappedValue }
            val chosenIndex = if (candidates.isNotEmpty()) candidates.random() else possibleIndices.random()
            
            lastSwappedValue = board[chosenIndex]
            
            // Swap
            board[emptyIndex] = board[chosenIndex]
            board[chosenIndex] = 0
            emptyIndex = chosenIndex
        }
        
        // Ensure it doesn't accidentally start solved
        if (board == solved) {
            // Swap two adjacent valid indices
            val possible = getAdjacentIndices(emptyIndex, size)
            if (possible.isNotEmpty()) {
                val idx = possible.first()
                board[emptyIndex] = board[idx]
                board[idx] = 0
            }
        }
        
        return board
    }

    private fun getAdjacentIndices(index: Int, size: Int): List<Int> {
        val list = mutableListOf<Int>()
        val r = index / size
        val c = index % size
        
        if (r > 0) list.add((r - 1) * size + c) // Up
        if (r < size - 1) list.add((r + 1) * size + c) // Down
        if (c > 0) list.add(r * size + (c - 1)) // Left
        if (c < size - 1) list.add(r * size + (c + 1)) // Right
        
        return list
    }

    // Try moving a tile by passing its Index in the _tiles list
    fun moveTileByIndex(index: Int): Boolean {
        if (_isCompleted.value || _isPaused.value) return false
        
        val size = _gridSize.value
        val tilesList = _tiles.value.toMutableList()
        val emptyIndex = tilesList.indexOf(0)
        
        // Check if adjacent orthogonally
        val r1 = index / size
        val c1 = index % size
        val r2 = emptyIndex / size
        val c2 = emptyIndex % size
        
        val isAdjacent = (abs(r1 - r2) == 1 && c1 == c2) || (abs(c1 - c2) == 1 && r1 == r2)
        
        if (isAdjacent) {
            // Swap!
            tilesList[emptyIndex] = tilesList[index]
            tilesList[index] = 0
            _tiles.value = tilesList
            _moves.value += 1
            _hintTileValue.value = null // Reset hint on move
            
            // Check solved!
            checkVictory()
            
            if (!_isCompleted.value) {
                saveCurrentGameState()
            }
            return true
        }
        return false
    }

    private fun checkVictory() {
        val size = _gridSize.value
        val solvedBoard = (1 until size * size).toList() + listOf(0)
        if (_tiles.value == solvedBoard) {
            _isCompleted.value = true
            stopTimer()
            
            // Log score to DB!
            viewModelScope.launch {
                repository.insertScore(
                    GameScore(
                        gridSize = size,
                        themeName = _themeName.value,
                        moves = _moves.value,
                        timeSecs = _timeSecs.value
                    )
                )
                repository.deleteActiveGame() // Deleted saved game as it's finished!
                checkSavedGamePresence()
            }
        }
    }

    // Smart Hint heuristic generator using sum of Manhattan Distances
    fun calculateHint() {
        if (_isCompleted.value || _isPaused.value) return
        val size = _gridSize.value
        val tilesList = _tiles.value
        val emptyIndex = tilesList.indexOf(0)
        
        val candidates = getAdjacentIndices(emptyIndex, size)
        if (candidates.isEmpty()) return
        
        var bestMoveIndex = -1
        var minDistanceSum = Int.MAX_VALUE
        
        for (candidateIdx in candidates) {
            // Simulate the board swap
            val simulatedList = tilesList.toMutableList()
            simulatedList[emptyIndex] = simulatedList[candidateIdx]
            simulatedList[candidateIdx] = 0
            
            // Compute sum of Manhattan distances
            var totalDistance = 0
            for (i in 0 until simulatedList.size) {
                val value = simulatedList[i]
                if (value != 0) {
                    val targetIdx = value - 1
                    
                    val currentR = i / size
                    val currentC = i % size
                    
                    val targetR = targetIdx / size
                    val targetC = targetIdx % size
                    
                    totalDistance += abs(currentR - targetR) + abs(currentC - targetC)
                }
            }
            
            if (totalDistance < minDistanceSum) {
                minDistanceSum = totalDistance
                bestMoveIndex = candidateIdx
            }
        }
        
        if (bestMoveIndex != -1) {
            _hintTileValue.value = tilesList[bestMoveIndex]
        }
    }

    // Auto load saved game
    fun loadSavedGame() {
        viewModelScope.launch {
            val saved = repository.getActiveGame()
            if (saved != null) {
                _gridSize.value = saved.gridSize
                _themeName.value = saved.themeName
                _moves.value = saved.moves
                _timeSecs.value = saved.timeSecs
                _isCompleted.value = saved.isCompleted
                _isPaused.value = false
                _hintTileValue.value = null
                
                // Parse CSV state string back into List<Int>
                val tileValues = saved.boardState.split(",").mapNotNull { it.toIntOrNull() }
                if (tileValues.size == saved.gridSize * saved.gridSize) {
                    _tiles.value = tileValues
                    _screenState.value = ScreenState.Playing
                    startTimer()
                } else {
                    // Corruption/fallback reset
                    startNewGame()
                }
            } else {
                startNewGame()
            }
        }
    }

    private fun saveCurrentGameState() {
        viewModelScope.launch {
            if (_tiles.value.isNotEmpty() && !_isCompleted.value) {
                val stateCSV = _tiles.value.joinToString(",")
                repository.saveActiveGame(
                    GameState(
                        gridSize = _gridSize.value,
                        themeName = _themeName.value,
                        boardState = stateCSV,
                        moves = _moves.value,
                        timeSecs = _timeSecs.value,
                        isCompleted = _isCompleted.value
                    )
                )
                checkSavedGamePresence()
            }
        }
    }

    // Game lifecycle controls
    fun pauseGame() {
        if (!_isPaused.value && !_isCompleted.value) {
            _isPaused.value = true
            stopTimer()
            saveCurrentGameState()
        }
    }

    fun resumeGame() {
        if (_isPaused.value && !_isCompleted.value) {
            _isPaused.value = false
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timeSecs.value += 1
                // Auto-save every 10 seconds for robustness
                if (_timeSecs.value % 10 == 0) {
                    saveCurrentGameState()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun pauseTimerStateOnly() {
        stopTimer()
    }

    private fun resumeTimer() {
        if (_screenState.value == ScreenState.Playing && !_isPaused.value && !_isCompleted.value) {
            startTimer()
        }
    }

    fun restartCurrentGame() {
        startNewGame()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
