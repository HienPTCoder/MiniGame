package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PuzzleApplication
import com.example.data.LevelScore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

// ── Screen Phases ──────────────────────────────────────────────────────────
enum class GamePhase {
    MENU, PLAYING, PROCESSING, LEVEL_COMPLETE, GAME_OVER, LEADERBOARD
}

// ── Level Configs ──────────────────────────────────────────────────────────
data class LevelConfig(
    val level: Int,
    val name: String,
    val targetScore: Int,
    val maxMoves: Int
)

val LEVEL_CONFIGS = listOf(
    LevelConfig(1,  "Tập Sự",       500,   20),
    LevelConfig(2,  "Bước Đầu",     1000,  22),
    LevelConfig(3,  "Thử Sức",      1800,  22),
    LevelConfig(4,  "Leo Thang",    2800,  25),
    LevelConfig(5,  "Nóng Bỏng",   4000,  25),
    LevelConfig(6,  "Cực Đỉnh",    5500,  25),
    LevelConfig(7,  "Bùng Cháy",   7500,  28),
    LevelConfig(8,  "Hỗn Loạn",   10000,  28),
    LevelConfig(9,  "Thần Kinh",  13000,  25),
    LevelConfig(10, "NOVA MASTER",18000,  25)
)

// ── ViewModel ─────────────────────────────────────────────────────────────
class GameViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val GRID_ROWS  = 7
        const val GRID_COLS  = 7
        const val GEM_COLORS = 6

        // Gem encoding: 0=empty, 1-6=normal gem, 11-16=lightning, 21-26=bomb, 31-36=nova
        fun gemColor(v: Int): Int = if (v <= 0) 0 else ((v - 1) % 10) + 1
        fun gemType(v: Int): Int  = if (v <= 0) 0 else (v - 1) / 10
        fun makeGem(color: Int, type: Int = 0): Int = type * 10 + color
    }

    private val repository = (application as PuzzleApplication).repository

    // ── State Flows ──────────────────────────────────────────────────────
    private val _gamePhase = MutableStateFlow(GamePhase.MENU)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _board = MutableStateFlow<List<Int>>(emptyList())
    val board: StateFlow<List<Int>> = _board.asStateFlow()

    private val _selectedIndex = MutableStateFlow<Int?>(null)
    val selectedIndex: StateFlow<Int?> = _selectedIndex.asStateFlow()

    private val _explodingIndices = MutableStateFlow<Set<Int>>(emptySet())
    val explodingIndices: StateFlow<Set<Int>> = _explodingIndices.asStateFlow()

    private val _shakeIndex = MutableStateFlow<Int?>(null)
    val shakeIndex: StateFlow<Int?> = _shakeIndex.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _movesLeft = MutableStateFlow(20)
    val movesLeft: StateFlow<Int> = _movesLeft.asStateFlow()

    private val _currentLevel = MutableStateFlow(1)
    val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

    private val _combo = MutableStateFlow(0)
    val combo: StateFlow<Int> = _combo.asStateFlow()

    private val _stars = MutableStateFlow(0)
    val stars: StateFlow<Int> = _stars.asStateFlow()

    private val _levelBestStars = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val levelBestStars: StateFlow<Map<Int, Int>> = _levelBestStars.asStateFlow()

    val topScores = repository.getTopScores().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private var processingJob: Job? = null

    init { loadLevelStars() }

    // ── Level Helpers ─────────────────────────────────────────────────────
    private fun loadLevelStars() {
        viewModelScope.launch {
            val map = mutableMapOf<Int, Int>()
            for (lvl in 1..LEVEL_CONFIGS.size) {
                repository.getBestScoreForLevel(lvl)?.let { map[lvl] = it.stars }
            }
            _levelBestStars.value = map
        }
    }

    fun getLevelConfig(level: Int = _currentLevel.value): LevelConfig =
        LEVEL_CONFIGS.getOrElse(level - 1) { LEVEL_CONFIGS.last() }

    fun setPhase(phase: GamePhase) { _gamePhase.value = phase }

    // ── Game Control ──────────────────────────────────────────────────────
    fun startLevel(level: Int) {
        processingJob?.cancel()
        _currentLevel.value = level
        val cfg = getLevelConfig(level)
        _score.value      = 0
        _movesLeft.value  = cfg.maxMoves
        _combo.value      = 0
        _stars.value      = 0
        _selectedIndex.value    = null
        _explodingIndices.value = emptySet()
        _shakeIndex.value       = null
        _board.value = generateBoard()
        _gamePhase.value = GamePhase.PLAYING
    }

    fun restartLevel() { startLevel(_currentLevel.value) }

    fun startNextLevel() {
        val next = _currentLevel.value + 1
        if (next <= LEVEL_CONFIGS.size) startLevel(next)
        else _gamePhase.value = GamePhase.MENU
    }

    // ── Board Generation ──────────────────────────────────────────────────
    private fun generateBoard(): List<Int> {
        var board: List<Int>
        var attempts = 0
        do {
            board = generateBoardNoMatches()
            attempts++
        } while (!hasValidMoves(board) && attempts < 20)
        return board
    }

    private fun generateBoardNoMatches(): List<Int> {
        val board = MutableList(GRID_ROWS * GRID_COLS) { 0 }
        for (i in board.indices) {
            val row = i / GRID_COLS
            val col = i % GRID_COLS
            val forbidden = mutableSetOf<Int>()
            // Horizontal: last 2 same color
            if (col >= 2) {
                val a = gemColor(board[i - 1]); val b = gemColor(board[i - 2])
                if (a != 0 && a == b) forbidden.add(a)
            }
            // Vertical: last 2 same color
            if (row >= 2) {
                val a = gemColor(board[i - GRID_COLS]); val b = gemColor(board[i - 2 * GRID_COLS])
                if (a != 0 && a == b) forbidden.add(a)
            }
            val available = (1..GEM_COLORS).filter { it !in forbidden }
            board[i] = if (available.isNotEmpty()) available.random() else (1..GEM_COLORS).random()
        }
        return board
    }

    // ── Player Input ──────────────────────────────────────────────────────
    fun onGemClick(index: Int) {
        if (_gamePhase.value != GamePhase.PLAYING) return
        val prev = _selectedIndex.value

        when {
            prev == null -> {
                _selectedIndex.value = index
            }
            prev == index -> {
                _selectedIndex.value = null
            }
            else -> {
                val r1 = prev / GRID_COLS;  val c1 = prev % GRID_COLS
                val r2 = index / GRID_COLS; val c2 = index % GRID_COLS
                // If not adjacent, reselect
                if (abs(r1 - r2) + abs(c1 - c2) != 1) {
                    _selectedIndex.value = index
                    return
                }

                // Simulate swap and validate
                val testBoard = _board.value.toMutableList()
                val tmp = testBoard[prev]; testBoard[prev] = testBoard[index]; testBoard[index] = tmp

                if (findAllMatches(testBoard).isEmpty()) {
                    // Invalid — shake animation
                    _selectedIndex.value = null
                    viewModelScope.launch {
                        _shakeIndex.value = prev
                        delay(500)
                        _shakeIndex.value = null
                    }
                } else {
                    // Valid swap!
                    _selectedIndex.value = null
                    _board.value = testBoard
                    _movesLeft.value -= 1
                    processingJob?.cancel()
                    processingJob = viewModelScope.launch { processChainReaction() }
                }
            }
        }
    }

    // ── Match Detection ───────────────────────────────────────────────────
    private fun findAllMatches(board: List<Int>): List<Set<Int>> {
        val groups = mutableListOf<Set<Int>>()

        // Horizontal
        for (row in 0 until GRID_ROWS) {
            var col = 0
            while (col < GRID_COLS) {
                val color = gemColor(board[row * GRID_COLS + col])
                if (color == 0) { col++; continue }
                var len = 1
                while (col + len < GRID_COLS && gemColor(board[row * GRID_COLS + col + len]) == color) len++
                if (len >= 3) groups.add((0 until len).map { row * GRID_COLS + col + it }.toSet())
                col += len
            }
        }

        // Vertical
        for (col in 0 until GRID_COLS) {
            var row = 0
            while (row < GRID_ROWS) {
                val color = gemColor(board[row * GRID_COLS + col])
                if (color == 0) { row++; continue }
                var len = 1
                while (row + len < GRID_ROWS && gemColor(board[(row + len) * GRID_COLS + col]) == color) len++
                if (len >= 3) groups.add((0 until len).map { (row + it) * GRID_COLS + col }.toSet())
                row += len
            }
        }

        return groups
    }

    // ── Chain Reaction Processor ──────────────────────────────────────────
    private suspend fun processChainReaction() {
        _gamePhase.value = GamePhase.PROCESSING
        var chainCount = 0

        while (true) {
            val matchGroups = findAllMatches(_board.value)
            if (matchGroups.isEmpty()) break

            chainCount++
            _combo.value = chainCount

            // Collect all indices to explode + determine power gem spawns
            val toExplode = mutableSetOf<Int>()
            val powerGemsToSpawn = mutableMapOf<Int, Int>() // index → gemValue

            for (group in matchGroups) {
                toExplode.addAll(group)
                val color = gemColor(_board.value[group.first()])
                when {
                    group.size >= 5 -> {
                        val spawnIdx = group.elementAt(group.size / 2)
                        powerGemsToSpawn[spawnIdx] = makeGem(color, 3) // Nova
                    }
                    group.size == 4 -> {
                        val spawnIdx = group.elementAt(1)
                        powerGemsToSpawn[spawnIdx] = makeGem(color, 1) // Lightning
                    }
                }
            }

            // Activate power gems that are caught in the explosion
            val extraExplode = mutableSetOf<Int>()
            for (idx in toExplode) {
                val v = _board.value[idx]
                when (gemType(v)) {
                    1 -> { // Lightning — clear entire row
                        val row = idx / GRID_COLS
                        for (c in 0 until GRID_COLS) extraExplode.add(row * GRID_COLS + c)
                    }
                    2 -> { // Bomb — clear 3×3
                        val row = idx / GRID_COLS; val col = idx % GRID_COLS
                        for (dr in -1..1) for (dc in -1..1) {
                            val nr = row + dr; val nc = col + dc
                            if (nr in 0 until GRID_ROWS && nc in 0 until GRID_COLS)
                                extraExplode.add(nr * GRID_COLS + nc)
                        }
                    }
                    3 -> { // Nova — clear all gems of same base color
                        val color = gemColor(v)
                        _board.value.forEachIndexed { i, gv -> if (gemColor(gv) == color) extraExplode.add(i) }
                    }
                }
            }

            val allExploding = toExplode + extraExplode
            _explodingIndices.value = allExploding

            // Score: 50 pts per gem × chain multiplier
            val gained = allExploding.size * 50 * chainCount
            _score.value += gained

            delay(320) // Explosion animation window

            // Apply board changes
            val newBoard = _board.value.toMutableList()
            allExploding.forEach { newBoard[it] = 0 }
            powerGemsToSpawn.forEach { (idx, gem) -> newBoard[idx] = gem }

            _board.value = newBoard.toList()
            _explodingIndices.value = emptySet()

            delay(80)

            // Gravity: gems fall down
            _board.value = applyGravity(_board.value.toMutableList())
            delay(220)

            // Fill empty slots with new gems
            _board.value = fillEmpty(_board.value.toMutableList())
            delay(180)
        }

        _combo.value = 0

        // Deadlock detection — auto-shuffle if no valid moves
        if (!hasValidMoves(_board.value)) {
            delay(200)
            var shuffled = _board.value.toMutableList()
            var attempts = 0
            do {
                shuffled.shuffle()
                attempts++
            } while (!hasValidMoves(shuffled) && attempts < 30)
            _board.value = shuffled.toList()
        }

        // Check win / lose
        val cfg = getLevelConfig()
        _gamePhase.value = when {
            _score.value >= cfg.targetScore -> {
                val earnedStars = when {
                    _score.value >= cfg.targetScore * 2      -> 3
                    _score.value >= (cfg.targetScore * 1.5).toInt() -> 2
                    else -> 1
                }
                _stars.value = earnedStars
                viewModelScope.launch {
                    repository.insertScore(
                        LevelScore(
                            level = _currentLevel.value,
                            score = _score.value,
                            stars = earnedStars,
                            moves = cfg.maxMoves - _movesLeft.value
                        )
                    )
                    loadLevelStars()
                }
                GamePhase.LEVEL_COMPLETE
            }
            _movesLeft.value <= 0 -> GamePhase.GAME_OVER
            else                   -> GamePhase.PLAYING
        }
    }

    // ── Board Helpers ─────────────────────────────────────────────────────
    private fun applyGravity(board: MutableList<Int>): List<Int> {
        for (col in 0 until GRID_COLS) {
            val nonEmpty = (0 until GRID_ROWS)
                .map { row -> board[row * GRID_COLS + col] }
                .filter { it != 0 }
            val empties = GRID_ROWS - nonEmpty.size
            for (row in 0 until GRID_ROWS) {
                board[row * GRID_COLS + col] = if (row < empties) 0 else nonEmpty[row - empties]
            }
        }
        return board
    }

    private fun fillEmpty(board: MutableList<Int>): List<Int> {
        for (i in board.indices) {
            if (board[i] == 0) board[i] = (1..GEM_COLORS).random()
        }
        return board
    }

    private fun hasValidMoves(board: List<Int>): Boolean {
        for (i in board.indices) {
            val row = i / GRID_COLS; val col = i % GRID_COLS
            // Swap right
            if (col < GRID_COLS - 1) {
                val test = board.toMutableList()
                val tmp = test[i]; test[i] = test[i + 1]; test[i + 1] = tmp
                if (findAllMatches(test).isNotEmpty()) return true
            }
            // Swap down
            if (row < GRID_ROWS - 1) {
                val test = board.toMutableList()
                val tmp = test[i]; test[i] = test[i + GRID_COLS]; test[i + GRID_COLS] = tmp
                if (findAllMatches(test).isNotEmpty()) return true
            }
        }
        return false
    }

    fun clearHistory() { viewModelScope.launch { repository.clearHistory() } }
}
