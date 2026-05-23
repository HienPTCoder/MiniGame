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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GameScore
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modern list of modern curated animal emojis for our Cute Theme
private val EMOJI_LIST = listOf(
    "🐼", "🐱", "🐶", "🦊", "🦁", "🐨", "🐯", "🐸",
    "🐙", "🐵", "🐧", "🐣", "🦄", "🦉", "🐳", "🐊",
    "🥑", "🍓", "🍉", "🍒", "🍩", "🍕", "🎈", "🚀"
)

// Particle metadata helper for victory confetti simulation
private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var size: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val color: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Slate900, Color(0xFF1E1B4B)) // Premium deep indigo slate background
                                )
                            )
                            .padding(innerPadding)
                    ) {
                        GameNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun GameNavigation(gameViewModel: GameViewModel = viewModel()) {
    val screenState by gameViewModel.screenState.collectAsStateWithLifecycle()
    
    // Refresh check to ensure active saved game state status is accurate on enter
    LaunchedEffect(screenState) {
        gameViewModel.checkSavedGamePresence()
    }

    AnimatedContent(
        targetState = screenState,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "screen_navigation"
    ) { state ->
        when (state) {
            ScreenState.Menu -> MainMenuScreen(gameViewModel)
            ScreenState.Playing -> GamePlayScreen(gameViewModel)
            ScreenState.Scores -> LeaderboardScreen(gameViewModel)
        }
    }
}

// ----------------------------------------------------
// SCREEN 1: MAIN MENU
// ----------------------------------------------------
@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    val selectedSize by viewModel.gridSize.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.themeName.collectAsStateWithLifecycle()
    val hasSavedGame by viewModel.hasActiveSavedGame.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Graphic Title Visual Card representing Zen Tiles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF312E81), Slate800),
                        radius = 280f
                    )
                )
                .border(1.5.dp, Indigo500.copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Interactive Mini-Grid Graphic Representation Logo
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Indigo500))
                    Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Teal400))
                    Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Amber400))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ZEN TILE",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = Color.White
                )
                Text(
                    text = "Trải nghiệm xếp gạch thư giãn & trí tuệ",
                    fontSize = 12.sp,
                    color = Slate300,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Selection Section 1: Board Dimensions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "KÍCH THƯỚC BÀN CHƠI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Teal400,
                    letterSpacing = 1.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        Triple(3, "3x3", "Dễ"),
                        Triple(4, "4x4", "Vừa"),
                        Triple(5, "5x5", "Khó")
                    )
                    options.forEach { (size, label, desc) ->
                        val isSelected = selectedSize == size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Indigo500 else Slate900)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Teal400 else Slate700,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (viewModel.hapticEnabled.value) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    viewModel.selectGridSize(size)
                                }
                                .padding(vertical = 12.dp)
                                .testTag("size_$label"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Slate200
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Slate100 else Slate500
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selection Section 2: Themes Selectors
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CHỦ ĐỀ GIAO DIỆN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Teal400,
                    letterSpacing = 1.sp
                )

                val themes = listOf(
                    Triple("Classic", "Số Cổ Điển", "🔢 1, 2, 3..."),
                    Triple("Emoji", "Bóng Thú Emoji", "🐱 🐶 🦊..."),
                    Triple("Gradient", "Sắc Màu Gradient", "🎨 Cực Thư Giãn")
                )

                themes.forEach { (thKey, title, subtitle) ->
                    val isSelected = selectedTheme == thKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Slate700 else Slate900)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) Teal400 else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (viewModel.hapticEnabled.value) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                viewModel.selectTheme(thKey)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("theme_$thKey"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Slate200
                            )
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = if (isSelected) Slate100 else Slate500
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (viewModel.hapticEnabled.value) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                viewModel.selectTheme(thKey)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Teal400,
                                unselectedColor = Slate500
                            )
                        )
                    }
                    if (thKey != "Gradient") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CTA Section Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Continuation / Resume Game Button
            if (hasSavedGame) {
                Button(
                    onClick = {
                        if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (viewModel.soundEnabled.value) view.playSoundEffect(SoundEffectConstants.CLICK)
                        viewModel.loadSavedGame()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("resume_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Tiếp tục", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TIẾP TỤC VÁN CHƠI DANG DỞ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Primary Start Brand New Game Button
            Button(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (viewModel.soundEnabled.value) view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.startNewGame()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Teal400),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Bắt đầu", tint = Slate900)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BẮT ĐẦU VÁN CHƠI MỚI",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Slate900
                )
            }

            // Secondary Buttons Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // High Scores Leaderboard Secondary action
                OutlinedButton(
                    onClick = {
                        if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (viewModel.soundEnabled.value) view.playSoundEffect(SoundEffectConstants.CLICK)
                        viewModel.setScreenState(ScreenState.Scores)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("leaderboard_tab"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = borderStroke()
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Bảng Điểm", tint = Amber400)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BẢNG ĐIỂM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // In-Menu Sound/Haptic Quick Toggle Cards
                var showSettingsDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = {
                        if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSettingsDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = borderStroke()
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Cài đặt", tint = Slate300)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CÀI ĐẶT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showSettingsDialog) {
                    SettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(1.2.dp, Slate700)

// ----------------------------------------------------
// SETTINGS DIALOG VIEW
// ----------------------------------------------------
@Composable
fun SettingsDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    val showIndicesInGradient by viewModel.showIndicesInGradient.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Cấu Hình Trò Chơi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Divider(color = Slate700)

                // Sound Effect Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Âm Thanh Clinch", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Bật/Tắt hiệu ứng click", fontSize = 11.sp, color = Slate500)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.toggleSound() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Teal400)
                    )
                }

                // Haptic Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Rung Phản Hồi", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Rung khi di chuyển ô gạch", fontSize = 11.sp, color = Slate500)
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { viewModel.toggleHaptic() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Teal400)
                    )
                }

                // Gradient Hints Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Gợi Ý Thứ Tự Màu", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Hiển thị chỉ số mờ trên ô màu", fontSize = 11.sp, color = Slate500)
                    }
                    Switch(
                        checked = showIndicesInGradient,
                        onCheckedChange = { viewModel.toggleShowIndicesInGradient() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Teal400)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Đóng", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 2: ACTIVE GAME PLAY OVERVIEW
// ----------------------------------------------------
@Composable
fun GamePlayScreen(viewModel: GameViewModel) {
    val size by viewModel.gridSize.collectAsStateWithLifecycle()
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val moves by viewModel.moves.collectAsStateWithLifecycle()
    val timeLimitSecs by viewModel.timeSecs.collectAsStateWithLifecycle()
    val hasCompleted by viewModel.isCompleted.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val themeName by viewModel.themeName.collectAsStateWithLifecycle()
    val hintValue by viewModel.hintTileValue.collectAsStateWithLifecycle()
    val showIndices by viewModel.showIndicesInGradient.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    // Confirmation Alert
    var showScrambleAlert by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP TASKBAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quay về / Back button
            IconButton(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (viewModel.soundEnabled.value) view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.pauseGame()
                    viewModel.setScreenState(ScreenState.Menu)
                },
                modifier = Modifier.background(Slate800, CircleShape).testTag("back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Trở về", tint = Color.White)
            }

            // Title mode
            val vietThemeName = when (themeName) {
                "Classic" -> "Cơ Bản 🔢"
                "Emoji" -> "Thú Vui 🐱"
                else -> "Gradient 🎨"
            }
            Text(
                text = "${size}x${size} • $vietThemeName",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            // Dynamic Settings Gear inside game Play
            var showQuickSettings by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showQuickSettings = true
                },
                modifier = Modifier.background(Slate800, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Cấu hình nhanh", tint = Slate300)
            }

            if (showQuickSettings) {
                SettingsDialog(viewModel = viewModel, onDismiss = { showQuickSettings = false })
            }
        }

        // STATS CARD SYSTEM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Move Counter
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).background(Indigo500.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Số bước", tint = Indigo500, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(text = "SỐ BƯỚC", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                        Text(text = "$moves", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Ticker Count
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).background(Teal400.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Thời gian", tint = Teal400, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(text = "THỜI GIAN", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatSeconds(timeLimitSecs),
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Pause Play Toggle State Icon
            IconButton(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isPaused) viewModel.resumeGame() else viewModel.pauseGame()
                },
                modifier = Modifier
                    .size(54.dp)
                    .background(if (isPaused) Teal400 else Slate800, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Phone, // Handled pause icon fallback manually or use custom
                    contentDescription = "Tạm dừng",
                    tint = if (isPaused) Slate900 else Color.White
                )
            }
        }

        // TACTILE PUZZLE BOARD CONTAINER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .shadow(12.dp, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Normal Grid View Board
            LazyVerticalGrid(
                columns = GridCells.Fixed(size),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Slate800)
                    .border(2.dp, Slate700, RoundedCornerShape(28.dp))
                    .padding(12.dp)
                    .testTag("puzzle_grid"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(tiles) { index, tileValue ->
                    TileCell(
                        value = tileValue,
                        gridSize = size,
                        themeName = themeName,
                        isHint = tileValue == hintValue,
                        showIndexInGradient = showIndices,
                        onClick = {
                            viewModel.moveTileByIndex(index)
                        }
                    )
                }
            }

            // Play Pause Glass frosted sheet screen
            if (isPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate900.copy(alpha = 0.85f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.resumeGame()
                        }
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pulsing Icon
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "play_pulse"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, CircleShape)
                                .background(Teal400, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Tiếp tục",
                                tint = Slate900,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = "Trò Chơi Tạm Dừng",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Chạm vào bàn chơi để tiếp tục",
                            fontSize = 13.sp,
                            color = Slate300
                        )
                    }
                }
            }
        }

        // CONTROL BUTTONS TOOLBAR UNDER BOARD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Heuristic Hint button
            Button(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (viewModel.soundEnabled.value) view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.calculateHint()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("hint_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke()
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Gợi ý", tint = Amber400)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Gợi Ý", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }

            // Scramble Reset Button
            Button(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showScrambleAlert = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("reset_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke()
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Trộn lại", tint = Teal400)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Trộn Lại", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
        }

        // CONFIRMATION DIALOG ALERT FOR RESET
        if (showScrambleAlert) {
            AlertDialog(
                onDismissRequest = { showScrambleAlert = false },
                title = { Text(text = "Trộn Lại Bàn Ghép", fontWeight = FontWeight.Bold, color = Color.White) },
                text = { Text(text = "Bạn có chắc chắn muốn bỏ dở ván cũ và xáo bài ngẫu nhiên lại từ đầu?", color = Slate200) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showScrambleAlert = false
                            viewModel.startNewGame()
                        }
                    ) {
                        Text(text = "Có, Trộn Lại", color = Rose500, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showScrambleAlert = false }) {
                        Text(text = "Hủy", color = Slate300)
                    }
                },
                containerColor = Slate800,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // POP VICTORY OVERLAY ON COMPLETED OR WON
        if (hasCompleted) {
            VictoryDialog(viewModel = viewModel)
        }
    }
}

// ----------------------------------------------------
// TILE GRID INDIVIDUAL COMPOSABLE ITEM
// ----------------------------------------------------
@Composable
fun TileCell(
    value: Int,
    gridSize: Int,
    themeName: String,
    isHint: Boolean,
    showIndexInGradient: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    if (value == 0) {
        // Empty placeholder space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Slate900.copy(alpha = 0.4f))
                .border(1.2.dp, Brush.linearGradient(listOf(Slate700, Color.Transparent)), RoundedCornerShape(16.dp))
        )
    } else {
        // Hint Pulse animation
        val infiniteTransition = rememberInfiniteTransition(label = "hint_pulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hint_scale"
        )
        val scale = if (isHint) animatedScale else 1f

        // Border highlighting for hint options
        val borderHighlight = if (isHint) {
            Brush.sweepGradient(listOf(Amber400, Color.White, Amber400))
        } else {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent))
        }

        val borderThickness = if (isHint) 3.dp else 1.2.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scaleAnimation(scale)
                .shadow(if (isHint) 8.dp else 2.dp, RoundedCornerShape(16.dp))
                .background(getTileBrushGradient(value, gridSize, themeName))
                .border(borderThickness, borderHighlight, RoundedCornerShape(16.dp))
                .clickable {
                    onClick()
                    // Handled inside onclick callback
                }
                .testTag("tile_$value"),
            contentAlignment = Alignment.Center
        ) {
            when (themeName) {
                "Classic" -> {
                    Text(
                        text = value.toString(),
                        color = Color.White,
                        fontSize = when (gridSize) {
                            3 -> 32.sp
                            4 -> 26.sp
                            else -> 20.sp
                        },
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
                "Emoji" -> {
                    Text(
                        text = EMOJI_LIST.getOrElse(value - 1) { "🦊" },
                        fontSize = when (gridSize) {
                            3 -> 42.sp
                            4 -> 34.sp
                            else -> 26.sp
                        },
                        textAlign = TextAlign.Center
                    )
                }
                "Gradient" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showIndexInGradient) {
                            Text(
                                text = value.toString(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom scale extension
@Composable
fun Modifier.scaleAnimation(scale: Float): Modifier {
    return this.graphicsLayer(scaleX = scale, scaleY = scale)
}

@Composable
fun getTileBrushGradient(value: Int, gridSize: Int, themeName: String): Brush {
    return when (themeName) {
        "Classic" -> {
            Brush.verticalGradient(
                listOf(Slate600, Slate700)
            )
        }
        "Emoji" -> {
            // Warm playful gradient ranges based on tile identifier
            val colors = when (value % 4) {
                0 -> listOf(Color(0xFF818CF8), Color(0xFF4F46E5)) // Soft Indigo Blue
                1 -> listOf(Color(0xFFF472B6), Color(0xFFDB2777)) // Soft Pink
                2 -> listOf(Color(0xFF34D399), Color(0xFF059669)) // Soft Green
                else -> listOf(Color(0xFFFBBF24), Color(0xFFD97706)) // Soft Peach Amber
            }
            Brush.verticalGradient(colors)
        }
        "Gradient" -> {
            val baseColor = calculateColorForSpectrumGrid(value, gridSize)
            Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(
                        red = (baseColor.red + 0.1f).coerceIn(0f, 1f),
                        green = (baseColor.green + 0.1f).coerceIn(0f, 1f),
                        blue = (baseColor.blue + 0.1f).coerceIn(0f, 1f)
                    ),
                    baseColor,
                    baseColor.copy(
                        red = (baseColor.red - 0.1f).coerceIn(0f, 1f),
                        green = (baseColor.green - 0.1f).coerceIn(0f, 1f),
                        blue = (baseColor.blue - 0.1f).coerceIn(0f, 1f)
                    )
                )
            )
        }
        else -> Brush.verticalGradient(listOf(Slate700, Slate800))
    }
}

fun calculateColorForSpectrumGrid(value: Int, gridSize: Int): Color {
    val destIdx = value - 1
    val r = destIdx / gridSize
    val c = destIdx % gridSize
    
    val t_h = if (gridSize > 1) c.toFloat() / (gridSize - 1) else 0f
    val t_v = if (gridSize > 1) r.toFloat() / (gridSize - 1) else 0f
    
    // Smooth Tropical Gradient coordinates
    val topLeft = Color(0xFFFF5E62) // Peach Coral
    val topRight = Color(0xFFF12711) // Intense Red
    val bottomLeft = Color(0xFF6366F1) // Velvet Indigo
    val bottomRight = Color(0xFF00FFF0) // Turquoise Ice
    
    val top = blendTwoColors(topLeft, topRight, t_h)
    val bottom = blendTwoColors(bottomLeft, bottomRight, t_h)
    
    return blendTwoColors(top, bottom, t_v)
}

fun blendTwoColors(c1: Color, c2: Color, percentage: Float): Color {
    val r = c1.red + (c2.red - c1.red) * percentage
    val g = c1.green + (c2.green - c1.green) * percentage
    val b = c1.blue + (c2.blue - c1.blue) * percentage
    val a = c1.alpha + (c2.alpha - c1.alpha) * percentage
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
}

// ----------------------------------------------------
// DIALOG: VICTORY CELEBRATION CONGRATS CARD
// ----------------------------------------------------
@Composable
fun VictoryDialog(viewModel: GameViewModel) {
    val moves by viewModel.moves.collectAsStateWithLifecycle()
    val timeLimitSecs by viewModel.timeSecs.collectAsStateWithLifecycle()
    val themeName by viewModel.themeName.collectAsStateWithLifecycle()
    val size by viewModel.gridSize.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Confetti particles local states for realtime gravity simulation
    val confettiList = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            addAll(
                List(60) {
                    ConfettiParticle(
                        x = (0..1000).random().toFloat(),
                        y = -(50..400).random().toFloat(),
                        speedX = (-4..4).random().toFloat(),
                        speedY = (4..12).random().toFloat(),
                        size = (12..28).random().toFloat(),
                        rotation = (0..360).random().toFloat(),
                        rotationSpeed = (-4..4).random().toFloat(),
                        color = listOf(
                            Color(0xFFFF5E62), Color(0xFFFBBF24), Color(0xFF34D399),
                            Color(0xFF22D3EE), Color(0xFFA855F7), Color(0xFFEC4899)
                        ).random()
                    )
                }
            )
        }
    }

    // Ticker frame animation to gravity advance particles
    var frameTick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frameTime ->
                frameTick = frameTime
                confettiList.forEach { p ->
                    p.y += p.speedY
                    p.x += p.speedX
                    p.rotation += p.rotationSpeed
                    // Re-loop on bottom screen boundaries
                    if (p.y > 2200) {
                        p.y = -50f
                        p.x = (0..1000).random().toFloat()
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { /* Force choose menu items to close on victory to prevent state errors */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            // Live Floating Confetti Simulation on Canvas Layer background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasUnitsFactorX = size.toFloat() // Scale factor helper
                confettiList.forEach { p ->
                    // Map logical design sizes to layout canvas surface dimensions
                    val mappedX = (p.x / 1000f) * this.size.width
                    val mappedY = p.y // Keep simple pixel falls
                    
                    rotate(p.rotation, pivot = Offset(mappedX, mappedY)) {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(mappedX - p.size/2, mappedY - p.size/2),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size)
                        )
                    }
                }
            }

            // Beautiful interactive dialogue presentation
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = androidx.compose.foundation.BorderStroke(2.dp, Teal400),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Trophy visual representation
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(Amber400.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, Amber400, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏆", fontSize = 42.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CHÚC MỪNG!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Teal400,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bạn đã hoàn thành thử thách xuất sắc!",
                            fontSize = 13.sp,
                            color = Slate300,
                            textAlign = TextAlign.Center
                        )
                    }

                    Divider(color = Slate700)

                    // Results block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Moves card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Tổng Bước", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                Text(text = "$moves", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }

                        // Time Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Thời Gian", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                Text(text = formatSeconds(timeLimitSecs), fontSize = 20.sp, color = Teal400, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons CTA block
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startNewGame()
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal400),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "VÁN GHÉP MỚI", fontWeight = FontWeight.Black, color = Slate900)
                        }

                        OutlinedButton(
                            onClick = {
                                if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setScreenState(ScreenState.Menu)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = borderStroke()
                        ) {
                            Text(text = "Về Trang Chủ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 3: HIGH SCORES LEADERBOARD
// ----------------------------------------------------
@Composable
fun LeaderboardScreen(viewModel: GameViewModel) {
    var specTabSize by remember { mutableStateOf(4) } // 3x3, 4x4, 5x5 tabs
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    // Observe Room database reactive lists
    val list3 by viewModel.bestScores3x3.collectAsStateWithLifecycle()
    val list4 by viewModel.bestScores4x4.collectAsStateWithLifecycle()
    val list5 by viewModel.bestScores5x5.collectAsStateWithLifecycle()

    val currentScores = when (specTabSize) {
        3 -> list3
        4 -> list4
        else -> list5
    }

    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Task bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (viewModel.soundEnabled.value) view.playSoundEffect(SoundEffectConstants.CLICK)
                    viewModel.setScreenState(ScreenState.Menu)
                },
                modifier = Modifier.background(Slate800, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Trở về", tint = Color.White)
            }

            Text(
                text = "KỶ LỤC ZEN TILE",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 18.sp,
                color = Color.White
            )

            // Clear scores trash can bin icon
            IconButton(
                onClick = {
                    if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showClearConfirm = true
                },
                modifier = Modifier.background(Slate800, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa lịch sử", tint = Rose500)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Slate800)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sizes = listOf(3, 4, 5)
            sizes.forEach { sizeValue ->
                val isSelected = specTabSize == sizeValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Indigo500 else Color.Transparent)
                        .clickable {
                            if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (viewModel.soundEnabled.value) view.playSoundEffect(SoundEffectConstants.CLICK)
                            specTabSize = sizeValue
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${sizeValue}x${sizeValue}",
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Slate300,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Records list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Slate800)
                .border(1.2.dp, Slate700, RoundedCornerShape(24.dp))
                .padding(14.dp)
        ) {
            if (currentScores.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🧘", fontSize = 48.sp)
                    Text(
                        text = "Chưa có kỷ lục nào cả",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Hãy hoàn thành ván ghép để lưu kỷ lục của bạn!",
                        fontSize = 12.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    lazyListItemsIndexed(currentScores) { index, item ->
                        ScoreRow(rank = index + 1, score = item)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Return button CTA
        Button(
            onClick = {
                if (viewModel.hapticEnabled.value) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setScreenState(ScreenState.Menu)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "Trở Về Trang Chủ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // CONFIRM CLEAN HISTORY DIALOG ALERT
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(text = "Xóa Lịch Sử Kỷ Lục", fontWeight = FontWeight.Bold, color = Color.White) },
                text = { Text(text = "Hành động này sẽ xóa toàn bộ danh sách kỷ lục hiện có và không thể phục hồi. Bạn chắc chắn?", color = Slate200) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearConfirm = false
                            viewModel.clearHistory()
                        }
                    ) {
                        Text(text = "Xóa Toàn Bộ", color = Rose500, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text(text = "Hủy", color = Slate300)
                    }
                },
                containerColor = Slate800,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ----------------------------------------------------
// INDIVIDUAL SCORE ROW CARD WRAPPER
// ----------------------------------------------------
@Composable
fun ScoreRow(rank: Int, score: GameScore) {
    val rankBadge = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "🍀"
    }

    val vietThemeLabel = when (score.themeName) {
        "Classic" -> "Cơ Bản"
        "Emoji" -> "Thú Vui"
        else -> "Gradient"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = rankBadge, fontSize = 20.sp)
                Column {
                    Text(
                        text = formatDateString(score.timestamp),
                        fontSize = 11.sp,
                        color = Slate500
                    )
                    Text(
                        text = "Chủ đề: $vietThemeLabel",
                        fontSize = 12.sp,
                        color = Slate300
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatSeconds(score.timeSecs),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Teal400
                )
                Text(
                    text = "${score.moves} bước ghép",
                    fontSize = 11.sp,
                    color = Slate300
                )
            }
        }
    }
}

// ----------------------------------------------------
// UTILITY STRING BUILDERS
// ----------------------------------------------------
fun formatSeconds(totalSecs: Int): String {
    val m = totalSecs / 60
    val s = totalSecs % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

fun formatDateString(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
