package com.example

import android.os.Bundle
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as lazyListItemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.data.LevelScore
import com.example.ui.theme.*
import com.example.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

// ── Confetti particle ─────────────────────────────────────────────────────
private data class NovaSpark(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var rot: Float, var rotV: Float,
    val color: Color
)

// ── Gem visual data ────────────────────────────────────────────────────────
private val GEM_BRUSHES: List<Brush> = listOf(
    Brush.verticalGradient(listOf(Color(0xFFFF6B7A), Color(0xFF8B0000))),  // 1 Ruby
    Brush.verticalGradient(listOf(Color(0xFF40D9FF), Color(0xFF003A8C))),  // 2 Sapphire
    Brush.verticalGradient(listOf(Color(0xFF5AFFA0), Color(0xFF006400))),  // 3 Emerald
    Brush.verticalGradient(listOf(Color(0xFFFFE566), Color(0xFF7A5000))),  // 4 Amber
    Brush.verticalGradient(listOf(Color(0xFFE96FFF), Color(0xFF4A0080))),  // 5 Amethyst
    Brush.verticalGradient(listOf(Color(0xFFFF8EC6), Color(0xFF880040))),  // 6 Rose
)

private val GEM_GLOW_COLORS = listOf(
    Color(0xFFFF4757), // Ruby
    Color(0xFF00C6FF), // Sapphire
    Color(0xFF38EF7D), // Emerald
    Color(0xFFFFD200), // Amber
    Color(0xFFDA22FF), // Amethyst
    Color(0xFFFF6CAB), // Rose
)

private val GEM_ICONS   = listOf("♦", "✦", "▲", "★", "⬡", "♥")
private val POWER_ICONS = mapOf(1 to "⚡", 2 to "💥", 3 to "✺")

// ── Particle confetti colors ───────────────────────────────────────────────
private val SPARK_COLORS = listOf(
    Color(0xFFFF4757), Color(0xFFFFD200), Color(0xFF38EF7D),
    Color(0xFF00C6FF), Color(0xFFDA22FF), Color(0xFFFF6CAB),
    Color(0xFFFFFFFF), Color(0xFF00F5FF)
)

// ─────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            com.example.ui.theme.MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(SpaceDeep, SpaceMid, Color(0xFF0A1020)))
                        )
                ) {
                    GameNavigation()
                }
            }
        }
    }
}

// ── Navigation ────────────────────────────────────────────────────────────
@Composable
fun GameNavigation(vm: GameViewModel = viewModel()) {
    val phase by vm.gamePhase.collectAsStateWithLifecycle()
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            fadeIn(tween(280)) togetherWith fadeOut(tween(220))
        },
        label = "nav"
    ) { p ->
        when (p) {
            GamePhase.MENU, GamePhase.LEADERBOARD -> MenuScreen(vm)
            GamePhase.PLAYING,
            GamePhase.PROCESSING,
            GamePhase.LEVEL_COMPLETE,
            GamePhase.GAME_OVER     -> GameScreen(vm)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// SCREEN 1 — MAIN MENU
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun MenuScreen(vm: GameViewModel) {
    val phase          by vm.gamePhase.collectAsStateWithLifecycle()
    val levelBestStars by vm.levelBestStars.collectAsStateWithLifecycle()
    val topScores      by vm.topScores.collectAsStateWithLifecycle()
    val haptic         = LocalHapticFeedback.current
    val view           = LocalView.current

    val showLeaderboard = phase == GamePhase.LEADERBOARD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // ── Logo ─────────────────────────────────────────────────────────
        NovaBlastLogo()

        Spacer(Modifier.height(20.dp))

        // ── Tab toggle ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A1628))
                .border(1.dp, SpaceBorder, RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(false, true).forEach { isLb ->
                val sel = showLeaderboard == isLb
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (sel) Color(0xFF1A3A7E) else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.setPhase(if (isLb) GamePhase.LEADERBOARD else GamePhase.MENU)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLb) "🏆  Bảng Xếp Hạng" else "🎮  Chọn Level",
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (sel) NeonCyan else Color(0xFF6B8EC0)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!showLeaderboard) {
            // ── Level Grid ────────────────────────────────────────────────
            Text(
                text = "CHỌN LEVEL",
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A7EC0),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(10.dp))

            val levelChunks = LEVEL_CONFIGS.chunked(2)
            levelChunks.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { cfg ->
                        LevelButton(
                            cfg = cfg,
                            bestStars = levelBestStars[cfg.level] ?: 0,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                vm.startLevel(cfg.level)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        } else {
            // ── Leaderboard ───────────────────────────────────────────────
            Text(
                text = "TOP 20 KỶ LỤC",
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A7EC0),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(10.dp))

            if (topScores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌌", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Chưa có kỷ lục nào", color = Color(0xFF3A5A8E), fontWeight = FontWeight.Bold)
                        Text("Hãy bắt đầu chơi và lập kỷ lục!", fontSize = 12.sp, color = Color(0xFF2A4A6E))
                    }
                }
            } else {
                topScores.forEachIndexed { index, score ->
                    LeaderboardRow(rank = index + 1, score = score)
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            if (topScores.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.clearHistory()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4757)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4757).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Xóa Lịch Sử", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun NovaBlastLogo() {
    // Animated glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF0D2060), Color(0xFF060A18)),
                    radius = 500f
                )
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = glowAlpha), NeonBlue.copy(alpha = glowAlpha))),
                RoundedCornerShape(28.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Gem row visual
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                GEM_GLOW_COLORS.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(c)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "NOVA",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = NeonCyan,
                letterSpacing = 6.sp
            )
            Text(
                text = "BLAST",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 8.sp
            )
            Text(
                text = "Match-3 Năng Lượng Vũ Trụ",
                fontSize = 11.sp,
                color = Color(0xFF4A7EC0),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun LevelButton(cfg: LevelConfig, bestStars: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isCompleted = bestStars > 0
    val bgColor = when {
        cfg.level == 10 -> Brush.linearGradient(listOf(Color(0xFF1A0040), Color(0xFF0A0020)))
        isCompleted     -> Brush.linearGradient(listOf(Color(0xFF0A1E40), Color(0xFF060E25)))
        else            -> Brush.linearGradient(listOf(Color(0xFF0A1628), Color(0xFF060A18)))
    }
    val borderColor = when {
        cfg.level == 10 -> Color(0xFFDA22FF)
        isCompleted     -> NeonCyan.copy(alpha = 0.5f)
        else            -> SpaceBorder.copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (cfg.level == 10) Color(0xFF2A0060) else Color(0xFF0D1F45)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cfg.level.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (cfg.level == 10) Color(0xFFDA22FF) else NeonCyan
                    )
                }
                // Stars
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { i ->
                        Text(
                            text = "★",
                            fontSize = 11.sp,
                            color = if (i < bestStars) Color(0xFFFFD200) else Color(0xFF1A2A4A)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = cfg.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatScore(cfg.targetScore)} pts • ${cfg.maxMoves} bước",
                fontSize = 10.sp,
                color = Color(0xFF4A7EC0)
            )
        }
    }
}

@Composable
fun LeaderboardRow(rank: Int, score: LevelScore) {
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank" }
    val cfg = LEVEL_CONFIGS.getOrElse(score.level - 1) { LEVEL_CONFIGS.last() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF081428))
            .border(1.dp, SpaceBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = medal, fontSize = if (rank <= 3) 18.sp else 13.sp)
            Column {
                Text(
                    text = "Lv.${score.level} — ${cfg.name}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = formatDate(score.timestamp) + " • ${score.moves} bước",
                    fontSize = 10.sp,
                    color = Color(0xFF3A5A8E)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatScore(score.score),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = NeonCyan
            )
            Row {
                repeat(3) { i ->
                    Text("★", fontSize = 10.sp, color = if (i < score.stars) Color(0xFFFFD200) else Color(0xFF1A2A4A))
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// SCREEN 2 — GAME PLAY
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun GameScreen(vm: GameViewModel) {
    val board           by vm.board.collectAsStateWithLifecycle()
    val selectedIndex   by vm.selectedIndex.collectAsStateWithLifecycle()
    val explodingIndices by vm.explodingIndices.collectAsStateWithLifecycle()
    val shakeIndex      by vm.shakeIndex.collectAsStateWithLifecycle()
    val score           by vm.score.collectAsStateWithLifecycle()
    val movesLeft       by vm.movesLeft.collectAsStateWithLifecycle()
    val currentLevel    by vm.currentLevel.collectAsStateWithLifecycle()
    val combo           by vm.combo.collectAsStateWithLifecycle()
    val stars           by vm.stars.collectAsStateWithLifecycle()
    val phase           by vm.gamePhase.collectAsStateWithLifecycle()
    val haptic          = LocalHapticFeedback.current
    val view            = LocalView.current

    val cfg = vm.getLevelConfig(currentLevel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Top Bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.setPhase(GamePhase.MENU)
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF0A1628), CircleShape)
                    .border(1.dp, SpaceBorder, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Về Menu", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LEVEL ${cfg.level}",
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = Color(0xFF4A7EC0),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = cfg.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.restartLevel()
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF0A1628), CircleShape)
                    .border(1.dp, SpaceBorder, CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Chơi lại", tint = Color(0xFF4A7EC0), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── HUD: Score + Moves ────────────────────────────────────────────
        GameHUD(score = score, targetScore = cfg.targetScore, movesLeft = movesLeft, maxMoves = cfg.maxMoves)

        Spacer(Modifier.height(8.dp))

        // ── Combo Indicator ───────────────────────────────────────────────
        // Use AnimatedContent (no ColumnScope receiver conflict) instead of AnimatedVisibility
        AnimatedContent(
            targetState = combo,
            transitionSpec = {
                (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn())
                    .togetherWith(scaleOut() + fadeOut(tween(400)))
            },
            label = "combo_anim"
        ) { c ->
            if (c > 1) {
                ComboLabel(c)
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Gem Board ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF060E22))
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(NeonCyan.copy(0.4f), NeonBlue.copy(0.2f))),
                    RoundedCornerShape(24.dp)
                )
                .padding(8.dp)
        ) {
            if (board.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GameViewModel.GRID_COLS),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(board) { index, gemValue ->
                        GemCell(
                            gemValue   = gemValue,
                            isSelected = selectedIndex == index,
                            isExploding = index in explodingIndices,
                            isShaking  = shakeIndex == index,
                            onClick    = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                vm.onGemClick(index)
                            }
                        )
                    }
                }
            }

            // Processing overlay (subtle so gems still visible)
            if (phase == GamePhase.PROCESSING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Block taps during processing */ }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Bottom hint ───────────────────────────────────────────────────
        Text(
            text = when (phase) {
                GamePhase.PROCESSING -> "⚡ Đang xử lý chain..."
                else -> "Chạm vào 2 viên kề nhau để đổi chỗ"
            },
            fontSize = 11.sp,
            color = Color(0xFF2A4A6E),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
    }

    // ── Overlay Dialogs ───────────────────────────────────────────────────
    if (phase == GamePhase.LEVEL_COMPLETE) {
        VictoryDialog(vm = vm, stars = stars, score = score, cfg = vm.getLevelConfig(currentLevel))
    }
    if (phase == GamePhase.GAME_OVER) {
        GameOverDialog(vm = vm, score = score, cfg = vm.getLevelConfig(currentLevel))
    }
}

@Composable
fun GameHUD(score: Int, targetScore: Int, movesLeft: Int, maxMoves: Int) {
    val animScore by animateIntAsState(targetValue = score, animationSpec = tween(400), label = "score")
    val progress  = (score.toFloat() / targetScore).coerceIn(0f, 1f)
    val animProg  by animateFloatAsState(targetValue = progress, animationSpec = tween(500), label = "prog")

    val movesColor = when {
        movesLeft <= 3  -> Color(0xFFFF4757)
        movesLeft <= 7  -> Color(0xFFFFD200)
        else            -> NeonCyan
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF080E20))
            .border(1.dp, SpaceBorder.copy(0.5f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Score row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text("ĐIỂM SỐ", fontSize = 9.sp, letterSpacing = 1.5.sp, color = Color(0xFF3A5A8E), fontWeight = FontWeight.Bold)
                Text(
                    text = formatScore(animScore),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SỐ BƯỚC", fontSize = 9.sp, letterSpacing = 1.5.sp, color = Color(0xFF3A5A8E), fontWeight = FontWeight.Bold)
                Text(
                    text = movesLeft.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = movesColor
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0D1F45))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animProg)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonBlue, NeonCyan)
                            )
                        )
                )
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 9.sp,
                color = NeonCyan.copy(0.8f),
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Mục tiêu: ${formatScore(targetScore)} pts",
            fontSize = 9.sp,
            color = Color(0xFF2A4060),
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun ComboLabel(combo: Int) {
    val label = when {
        combo >= 5 -> "🌀 ×$combo  N O V A  !!"
        combo >= 4 -> "💥 ×$combo  MEGA BLAST!"
        combo >= 3 -> "⚡ ×$combo  CHAIN!!"
        else       -> "✦ ×$combo  COMBO!"
    }
    val color = when {
        combo >= 5 -> Color(0xFFDA22FF)
        combo >= 4 -> Color(0xFFFF4757)
        combo >= 3 -> Color(0xFFFFD200)
        else       -> NeonCyan
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(0.15f))
            .border(1.dp, color.copy(0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
// GEM CELL
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun GemCell(
    gemValue: Int,
    isSelected: Boolean,
    isExploding: Boolean,
    isShaking: Boolean,
    onClick: () -> Unit
) {
    if (gemValue == 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(Color(0xFF040810))
        )
        return
    }

    val colorIdx = (GameViewModel.gemColor(gemValue) - 1).coerceIn(0, GEM_BRUSHES.lastIndex)
    val gemType  = GameViewModel.gemType(gemValue)
    val glow     = GEM_GLOW_COLORS[colorIdx]
    val icon     = GEM_ICONS[colorIdx]
    val powerIcon = POWER_ICONS[gemType]

    // Scale animation: selected → grow; exploding → shrink
    val targetScale = when {
        isExploding -> 0f
        isSelected  -> 1.13f
        else        -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isExploding) tween(280, easing = FastOutLinearInEasing)
                        else spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "gem_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isExploding) 0f else 1f,
        animationSpec = tween(260),
        label = "gem_alpha"
    )

    // Shake animation for invalid swap
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            shakeAnim.animateTo(0f, keyframes {
                durationMillis = 400
                0f    at 0
                (-9f) at 50
                9f    at 100
                (-7f) at 160
                7f    at 210
                (-4f) at 270
                0f    at 360
            })
        }
    }

    // Pulse for selected gem
    val infiniteTransition = rememberInfiniteTransition(label = "sel_pulse")
    val selectedGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "sel_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer(
                scaleX = scale, scaleY = scale,
                alpha = alpha,
                translationX = shakeAnim.value
            )
    ) {
        // Glow backdrop when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(glow.copy(alpha = 0.35f * selectedGlow))
            )
        }

        // Gem body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .shadow(
                    elevation = if (isSelected) 8.dp else 2.dp,
                    shape = CircleShape,
                    ambientColor = glow.copy(0.5f),
                    spotColor = glow.copy(0.8f)
                )
                .clip(CircleShape)
                .background(GEM_BRUSHES[colorIdx])
                .border(
                    width = if (isSelected) 2.dp else 0.8.dp,
                    color = if (isSelected) glow else glow.copy(0.3f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner highlight (glass shine)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(0.35f), Color.Transparent),
                            center = Offset(0.3f, 0.2f),
                            radius = 0.5f
                        )
                    )
            )

            // Gem icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (powerIcon != null) {
                    Text(
                        text = powerIcon,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = icon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(0.9f)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// VICTORY DIALOG
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun VictoryDialog(vm: GameViewModel, stars: Int, score: Int, cfg: LevelConfig) {
    val haptic = LocalHapticFeedback.current

    // Confetti particles
    val sparks = remember {
        mutableStateListOf<NovaSpark>().apply {
            addAll(List(70) {
                NovaSpark(
                    x = (0..1000).random().toFloat(),
                    y = -(100..600).random().toFloat(),
                    vx = (-5..5).random().toFloat(),
                    vy = (5..14).random().toFloat(),
                    size = (10..24).random().toFloat(),
                    rot = (0..360).random().toFloat(),
                    rotV = (-5..5).random().toFloat(),
                    color = SPARK_COLORS.random()
                )
            })
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                sparks.forEach { p ->
                    p.x += p.vx; p.y += p.vy; p.rot += p.rotV
                    if (p.y > 2400) { p.y = -80f; p.x = (0..1000).random().toFloat() }
                }
            }
        }
    }

    // Star reveal animation (one by one)
    var revealedStars by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(300)
        for (i in 1..stars) {
            revealedStars = i
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(350)
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Particle canvas layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                sparks.forEach { p ->
                    val mx = (p.x / 1000f) * size.width
                    rotate(p.rot, Offset(mx, p.y)) {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(mx - p.size / 2, p.y - p.size / 2),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size)
                        )
                    }
                }
            }

            // Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF080E20))
                    .border(2.dp, NeonCyan.copy(0.7f), RoundedCornerShape(28.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🏆", fontSize = 48.sp)
                Text(
                    text = "LEVEL ${cfg.level} HOÀN THÀNH!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonCyan,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                // Animated stars
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { i ->
                        val visible = i < revealedStars
                        val starScale by animateFloatAsState(
                            targetValue = if (visible) 1f else 0.2f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy),
                            label = "star$i"
                        )
                        Text(
                            text = "★",
                            fontSize = 38.sp,
                            color = if (visible) Color(0xFFFFD200) else Color(0xFF1A2A4A),
                            modifier = Modifier.graphicsLayer(scaleX = starScale, scaleY = starScale)
                        )
                    }
                }

                // Score + stars label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(label = "ĐIỂM ĐẠT", value = formatScore(score), color = NeonCyan, modifier = Modifier.weight(1f))
                    StatCard(label = "MỤC TIÊU", value = formatScore(cfg.targetScore), color = Color(0xFF4A7EC0), modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = SpaceBorder.copy(0.5f))

                // Action buttons
                if (cfg.level < LEVEL_CONFIGS.size) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.startNextLevel()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("LEVEL TIẾP THEO ▶", fontWeight = FontWeight.Black, color = Color(0xFF060A18), fontSize = 15.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.restartLevel()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Chơi Lại", fontWeight = FontWeight.Bold, fontSize = 13.sp) }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.setPhase(GamePhase.MENU)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4A7EC0)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Menu", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// GAME OVER DIALOG
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun GameOverDialog(vm: GameViewModel, score: Int, cfg: LevelConfig) {
    val haptic = LocalHapticFeedback.current
    val progress = (score.toFloat() / cfg.targetScore).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF080E20))
                .border(2.dp, Color(0xFFFF4757).copy(0.6f), RoundedCornerShape(28.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("💀", fontSize = 48.sp)
            Text(
                text = "HẾT BƯỚC!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF4757),
                letterSpacing = 2.sp
            )
            Text(
                text = "Bạn đạt ${formatScore(score)} / ${formatScore(cfg.targetScore)} điểm",
                fontSize = 13.sp,
                color = Color(0xFF4A7EC0),
                textAlign = TextAlign.Center
            )

            // Progress bar showing how close
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Đạt được ${(progress * 100).toInt()}% mục tiêu",
                    fontSize = 11.sp,
                    color = Color(0xFF3A5A8E)
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0D1F45))
                ) {
                    val animProg by animateFloatAsState(targetValue = progress, tween(600), label = "fail_prog")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animProg)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF4757), Color(0xFFFFD200))))
                    )
                }
            }

            HorizontalDivider(color = SpaceBorder.copy(0.5f))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.restartLevel()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4757)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("THỬ LẠI ↺", fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.setPhase(GamePhase.MENU)
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4A7EC0)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Về Menu", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// HELPERS
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF060C1A))
            .border(1.dp, SpaceBorder.copy(0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 9.sp, letterSpacing = 1.sp, color = Color(0xFF3A5A8E), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

fun formatScore(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}.${(n % 1_000_000) / 100_000}M"
    n >= 1_000     -> "${n / 1000}.${(n % 1000) / 100}K"
    else            -> n.toString()
}

fun formatDate(ts: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ts))
