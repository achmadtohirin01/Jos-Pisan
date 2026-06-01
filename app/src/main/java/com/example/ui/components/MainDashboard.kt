package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AudioPresetEntity
import com.example.data.AudioSettingsEntity
import com.example.data.RoutingEntity
import com.example.viewmodel.AudioViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: AudioViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val presets by viewModel.presetsState.collectAsStateWithLifecycle()
    val routing by viewModel.routingState.collectAsStateWithLifecycle()
    
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val volL by viewModel.volumeL.collectAsStateWithLifecycle()
    val volR by viewModel.volumeR.collectAsStateWithLifecycle()
    val spectrum by viewModel.spectrumFlow.collectAsStateWithLifecycle()
    val systemBypassActive by viewModel.isSystemAudioBypassActive.collectAsStateWithLifecycle()

    val currentTheme = ThemeRegistry.get(settings.selectedTheme)

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Display Toast/Popup parameters
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    // Scaffold for custom overlay styling and notch boundaries
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(currentTheme.bgMain),
        containerColor = currentTheme.bgMain,
        topBar = {
            // App Header with strict background matching and bottom border
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.bgHeader)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Brand Logo and Typography Pairings from designspec
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF2563EB))), shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speaker,
                                    contentDescription = "Speaker Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "BRO",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        letterSpacing = (-0.5).sp,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "AUDIO CROSS",
                                        color = currentTheme.primaryGlow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.SansSerif,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Text(
                                    text = "OBOE POWERED NATIVE PRO",
                                    fontSize = 7.sp,
                                    color = currentTheme.textSec,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        // Synth Play/Pause + Settings Actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Quick Source Selector Button
                            IconButton(
                                onClick = {
                                    viewModel.setSystemAudioBypassActive(!systemBypassActive)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (systemBypassActive) currentTheme.primaryGlow.copy(alpha = 0.25f) else currentTheme.bgCard,
                                    contentColor = if (systemBypassActive) currentTheme.primaryGlow else currentTheme.textSec
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Tangkap Audio Sistem Bypass",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Play/Pause synthesizer loop trigger
                            IconButton(
                                onClick = { viewModel.togglePlayback() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isPlaying) currentTheme.primaryGlow.copy(alpha = 0.25f) else currentTheme.bgCard,
                                    contentColor = if (isPlaying) currentTheme.primaryGlow else currentTheme.textMain
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause Synth Grid" else "Play Synth Grid",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Settings trigger
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = currentTheme.bgCard,
                                    contentColor = currentTheme.textMain
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings Icon"
                                )
                            }
                        }
                    }
                }
                Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            }
        },
        bottomBar = {
            // Footer Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.bgHeader)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                // Bottom Tab selection
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = currentTheme.bgHeader,
                    contentColor = currentTheme.primaryGlow,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = currentTheme.primaryGlow,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("MIXER & EQ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "Equalizer Tab", modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("FX & CROSSOVER", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.SettingsInputComponent, contentDescription = "FX & Crossover Tab", modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("ROUTING CH", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.DeviceHub, contentDescription = "Routing Matrix Tab", modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Banjarnegara Watermark footer text
                Text(
                    text = "◆   BRO AUDIO BANJARNEGARA   ◆",
                    color = currentTheme.textSec.copy(0.45f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    ) { innerPadding ->
        // Animated View Routing depending on tab selection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(currentTheme.bgMain)
        ) {
            when (selectedTab) {
                0 -> MainControlTab(
                    viewModel = viewModel,
                    settings = settings,
                    presets = presets,
                    volL = volL,
                    volR = volR,
                    spectrum = spectrum,
                    theme = currentTheme,
                    onSavePresetClick = { showSavePresetDialog = true }
                )
                1 -> AccessoriesTab(
                    viewModel = viewModel,
                    settings = settings,
                    theme = currentTheme
                )
                2 -> RoutingMatrixTab(
                    viewModel = viewModel,
                    routing = routing,
                    theme = currentTheme
                )
            }
        }
    }

    // 1. Settings Overlay / Dialog
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.bgCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, currentTheme.textSec.copy(0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "HARDWARE ENGINE CONFIG",
                        color = currentTheme.primaryGlow,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )

                    Divider(color = currentTheme.textSec.copy(0.15f))

                    // Input source configurations - SISTEM AUDIO BYPASS exactly as pictured
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, currentTheme.textSec.copy(0.12f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = currentTheme.bgCard
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    viewModel.setSystemAudioBypassActive(!systemBypassActive)
                                }
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsInputComponent,
                                        contentDescription = "Bypass Icon",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "SISTEM AUDIO BYPASS",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (systemBypassActive) Color(0xFF00E676)
                                            else Color(0xFF263238)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (systemBypassActive) "●  AKTIF" else "○  NONAKTIF",
                                        color = if (systemBypassActive) Color.Black else Color(0xFFB0BEC5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Text(
                                text = "Aplikasi akan memodifikasi suara perangkat secara global. Putar audio di aplikasi pihak ketiga seperti YouTube, Spotify, atau TikTok. Pengaturan Equalizer, Bass Boost, dan Reverb akan langsung berpengaruh secara real-time pada suara yang dihasilkan dari HP Anda!",
                                color = currentTheme.textSec,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Buffer size configurations
                    Column {
                        Text("AUDIO BUFFER DEPTH", color = currentTheme.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Lower buffer reduces response latency but might click", color = currentTheme.textSec, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(128, 256, 512, 1024).forEach { size ->
                                val active = settings.bufferSize == size
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) currentTheme.primaryGlow.copy(alpha = 0.2f) else currentTheme.bgMain)
                                        .border(1.dp, if (active) currentTheme.primaryGlow else currentTheme.textSec.copy(0.2f), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateBufferSize(size) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$size smp",
                                        color = if (active) currentTheme.primaryGlow else currentTheme.textMain,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Audio driver
                    Column {
                        Text("HARDWARE AUDIO DRIVER API", color = currentTheme.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("AAudio", "OpenSL ES").forEach { api ->
                                val active = settings.audioDriver == api
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) currentTheme.primaryGlow.copy(alpha = 0.2f) else currentTheme.bgMain)
                                        .border(1.dp, if (active) currentTheme.primaryGlow else currentTheme.textSec.copy(0.2f), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateAudioDriver(api) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = api,
                                        color = if (active) currentTheme.primaryGlow else currentTheme.textMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Visual studio theme engine
                    Column {
                        Text("VISUAL THEME INTERFACE", color = currentTheme.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("Glow in the Dark", "Blue Green", "Purple Neon", "Metallic").forEach { themeName ->
                            val active = settings.selectedTheme == themeName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) currentTheme.primaryGlow.copy(alpha = 0.15f) else currentTheme.bgMain)
                                    .clickable { viewModel.updateTheme(themeName) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (themeName) {
                                                    "Glow in the Dark" -> Color(0xFF00FF66)
                                                    "Blue Green" -> Color(0xFF00F0FF)
                                                    "Purple Neon" -> Color(0xFFD600FF)
                                                    else -> Color(0xFFB0BEC5)
                                                }
                                            )
                                    )
                                    Text(text = themeName, color = currentTheme.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (active) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = currentTheme.primaryGlow, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryGlow, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Save Preset Pop-Up Dialog
    if (showSavePresetDialog) {
        Dialog(onDismissRequest = { showSavePresetDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.bgCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, currentTheme.textSec.copy(0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SAVE EQUALIZATION PROFILE",
                        color = currentTheme.primaryGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    TextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        placeholder = { Text("Profile Name (e.g. My Studio Bass)") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = currentTheme.bgMain,
                            unfocusedContainerColor = currentTheme.bgMain,
                            focusedTextColor = currentTheme.textMain,
                            unfocusedTextColor = currentTheme.textMain,
                            focusedIndicatorColor = currentTheme.primaryGlow
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showSavePresetDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = currentTheme.textMain),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCEL")
                        }
                        Button(
                            onClick = {
                                if (newPresetName.isNotBlank()) {
                                    viewModel.createCustomPreset(newPresetName)
                                    newPresetName = ""
                                    showSavePresetDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryGlow, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ======================= Tab 1: Control Workspace =======================
@Composable
fun MainControlTab(
    viewModel: AudioViewModel,
    settings: AudioSettingsEntity,
    presets: List<AudioPresetEntity>,
    volL: Float,
    volR: Float,
    spectrum: FloatArray,
    theme: StudioThemeColors,
    onSavePresetClick: () -> Unit
) {
    val selectedPresetName by viewModel.selectedPresetName.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High fidelity spectrum visualization and VU in same row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Real-time FFT display
            FftSpectrumVisualizer(
                spectrum = spectrum,
                theme = theme,
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
            )

            // Dynamic twin level index blocks
            DualVuMeter(
                levelL = volL,
                levelR = volR,
                theme = theme,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            )
        }

        // Master Faders (L & R volumes) styled with premium custom radial-horizontal gradient background
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            theme.primaryGlow.copy(alpha = 0.08f),
                            Color(0xFF2563EB).copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, theme.primaryGlow.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Elegant rotary gain knob visual feedback decoration
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .border(2.dp, theme.primaryGlow.copy(alpha = 0.3f), CircleShape)
                        .background(theme.bgMain, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val avgVol = (settings.masterVolumeL + settings.masterVolumeR) / 2f
                    val rotationAngle = -135f + (avgVol * 270f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = rotationAngle }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(theme.primaryGlow, RoundedCornerShape(1.5.dp))
                                .align(Alignment.TopCenter)
                        )
                    }
                    Text(
                        text = "GAIN",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Volume slider columns on the right
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MASTER MAIN GAIN CONTROLLERS",
                        color = theme.textSec,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Volume Left
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("VOLUME L", color = theme.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${(settings.masterVolumeL * 100).roundToInt()}%", color = theme.primaryGlow, fontSize = 9.sp)
                            }
                            Slider(
                                value = settings.masterVolumeL,
                                onValueChange = { viewModel.updateMasterVolumes(it, settings.masterVolumeR) },
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.primaryGlow,
                                    activeTrackColor = theme.primaryGlow,
                                    inactiveTrackColor = theme.bgMain
                                )
                            )
                        }

                        // Volume Right
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("VOLUME R", color = theme.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${(settings.masterVolumeR * 100).roundToInt()}%", color = theme.primaryGlow, fontSize = 9.sp)
                            }
                            Slider(
                                value = settings.masterVolumeR,
                                onValueChange = { viewModel.updateMasterVolumes(settings.masterVolumeL, it) },
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.primaryGlow,
                                    activeTrackColor = theme.primaryGlow,
                                    inactiveTrackColor = theme.bgMain
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sub Mixer Sections (4 horizontal submixers: Sub, Low, Mid, High)
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.bgCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "4-BAND SIGNAL BUS MIXING",
                    color = theme.textSec,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                data class MixerChannel(val label: String, val value: Float, val color: Color, val onValueChange: (Float) -> Unit)
                val channels = listOf(
                    MixerChannel("SUB BASS (Deep Low)", settings.mixerSub, theme.primaryGlow) { v -> viewModel.updateMixerFaders(v, settings.mixerLow, settings.mixerMid, settings.mixerHigh) },
                    MixerChannel("LOW PASS (Vocals Body)", settings.mixerLow, theme.secondaryAccent) { v -> viewModel.updateMixerFaders(settings.mixerSub, v, settings.mixerMid, settings.mixerHigh) },
                    MixerChannel("MID RANGE (Presence)", settings.mixerMid, theme.textSec) { v -> viewModel.updateMixerFaders(settings.mixerSub, settings.mixerLow, v, settings.mixerHigh) },
                    MixerChannel("HIGH ACCENTS (Air/Beeps)", settings.mixerHigh, Color(0xFFF97316)) { v -> viewModel.updateMixerFaders(settings.mixerSub, settings.mixerLow, settings.mixerMid, v) }
                )

                channels.forEach { item ->
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.label, color = theme.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${(item.value * 100).roundToInt()}%", color = item.color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = item.value,
                            onValueChange = item.onValueChange,
                            colors = SliderDefaults.colors(
                                thumbColor = item.color,
                                activeTrackColor = item.color,
                                inactiveTrackColor = theme.bgMain
                            )
                        )
                    }
                }
            }
        }

        // Equalizers Module
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.bgCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDIO GRAPHIC EQUALIZER",
                        color = theme.textSec,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Profile preset selector
                    var showPresetsMenu by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = theme.bgMain,
                            modifier = Modifier.clickable { showPresetsMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    selectedPresetName.uppercase(),
                                    color = theme.primaryGlow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Presets", tint = theme.primaryGlow, modifier = Modifier.size(12.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showPresetsMenu,
                            onDismissRequest = { showPresetsMenu = false },
                            modifier = Modifier.background(theme.bgCard)
                        ) {
                            presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.name, color = theme.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.selectPreset(preset.name)
                                        showPresetsMenu = false
                                    }
                                )
                            }
                            Divider(color = theme.textSec.copy(0.15f))
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add custom preset", tint = theme.primaryGlow, modifier = Modifier.size(14.dp)) },
                                text = { Text("SAVE AS NEW...", color = theme.primaryGlow, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) },
                                onClick = {
                                    showPresetsMenu = false
                                    onSavePresetClick()
                                }
                            )
                        }
                    }
                }

                // Switch rack for bands setting
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(7, 15, 31).forEach { count ->
                        val active = settings.eqMode == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) theme.primaryGlow.copy(alpha = 0.2f) else theme.bgMain)
                                .border(1.dp, if (active) theme.primaryGlow else theme.textSec.copy(0.15f), RoundedCornerShape(6.dp))
                                .clickable { viewModel.updateEqMode(count) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$count BANDS",
                                color = if (active) theme.primaryGlow else theme.textMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Scrollable Graphic sliders corresponding to active bands
                val currentGains = viewModel.audioEngine.eqGains
                val currentFrequencies = when (settings.eqMode) {
                    7 -> listOf("60Hz", "150Hz", "400Hz", "1kHz", "2.5kHz", "6kHz", "15kHz")
                    15 -> listOf("25Hz", "40Hz", "63Hz", "100Hz", "160Hz", "250Hz", "400Hz", "630Hz", "1k", "1.6k", "2.5k", "4k", "6.3k", "10k", "16k")
                    else -> listOf(
                        "20", "25", "31.5", "40", "50", "63", "80", "100", "125", "160", "200", "250", "315", "400", "500", "630", "800",
                        "1k", "1.25k", "1.6k", "2k", "2.5k", "3.15k", "4k", "5k", "6.3k", "8k", "10k", "12.5k", "16k", "20k"
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (i in 0 until settings.eqMode) {
                        val gainVal = if (i < currentGains.size) currentGains[i] else 0.0f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(34.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${gainVal.roundToInt()} dB",
                                color = if (gainVal != 0f) theme.primaryGlow else theme.textSec,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            // Vertical Slider implementation for EQ
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .width(16.dp)
                            ) {
                                Slider(
                                    value = gainVal,
                                    onValueChange = { viewModel.updateEqSlider(i, it) },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = theme.primaryGlow,
                                        activeTrackColor = theme.primaryGlow,
                                        inactiveTrackColor = theme.bgMain
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .height(140.dp)
                                        .graphicsLayer {
                                            rotationZ = 270f
                                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                        }
                                )
                            }

                            Text(
                                text = currentFrequencies.getOrNull(i) ?: "",
                                color = theme.textMain,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================= Tab 2: Accessories Rack =======================
@Composable
fun AccessoriesTab(
    viewModel: AudioViewModel,
    settings: AudioSettingsEntity,
    theme: StudioThemeColors
) {
    val scrollState = rememberScrollState()

    // Real-time crossover bands powers (simulated in background filter)
    val subP by viewModel.bandPowerSub.collectAsStateWithLifecycle()
    val lowP by viewModel.bandPowerLow.collectAsStateWithLifecycle()
    val midP by viewModel.bandPowerMid.collectAsStateWithLifecycle()
    val highP by viewModel.bandPowerHigh.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Crossover Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.bgCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "4-WAY ACTIVE CROSSOVER (LR 24dB/oct)",
                            color = theme.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Splits output into Subwoofer, Low, Mid, and High channels",
                            color = theme.textSec,
                            fontSize = 8.sp
                        )
                    }
                    
                    // Bypass switch for Crossover
                    BypassTag(
                        bypassed = viewModel.audioEngine.crossoverBypass,
                        onClick = { viewModel.setBypassState("CROSSOVER", !viewModel.audioEngine.crossoverBypass) },
                        theme = theme
                    )
                }

                // Crossover curve visualizer graph
                CrossoverCurveVisualizer(
                    subOut = settings.crossSubOut,
                    lowOut = settings.crossLowOut,
                    midOut = settings.crossMidOut,
                    highOut = settings.crossHighOut,
                    subLowCut = settings.crossSubLowCutoff,
                    lowMidCut = settings.crossLowMidCutoff,
                    midHighCut = settings.crossMidHighCutoff,
                    theme = theme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                )

                // Cutoff frequency slider adjustments
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("COEFFICIENT CUTOFF ALIGNMENT", color = theme.textSec, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    
                    // Sub-Low cutoff cutoff slider (50Hz to 250Hz)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SUB/LOW X-OVER: ", color = theme.textMain, fontSize = 9.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = settings.crossSubLowCutoff,
                            onValueChange = { viewModel.updateCrossoverCutoffs(it, settings.crossLowMidCutoff, settings.crossMidHighCutoff) },
                            valueRange = 50f..250f,
                            colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow),
                            modifier = Modifier.weight(1f)
                        )
                        Text("${settings.crossSubLowCutoff.roundToInt()} Hz", color = theme.primaryGlow, fontSize = 9.sp, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
                    }

                    // Low-Mid cutoff (200Hz to 1200Hz)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LOW/MID X-OVER: ", color = theme.textMain, fontSize = 9.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = settings.crossLowMidCutoff,
                            onValueChange = { viewModel.updateCrossoverCutoffs(settings.crossSubLowCutoff, it, settings.crossMidHighCutoff) },
                            valueRange = 250f..1200f,
                            colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow),
                            modifier = Modifier.weight(1f)
                        )
                        Text("${settings.crossLowMidCutoff.roundToInt()} Hz", color = theme.primaryGlow, fontSize = 9.sp, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
                    }

                    // Mid-High cutoff (1500Hz to 8000Hz)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MID/HIGH X-OVER: ", color = theme.textMain, fontSize = 9.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = settings.crossMidHighCutoff,
                            onValueChange = { viewModel.updateCrossoverCutoffs(settings.crossSubLowCutoff, settings.crossLowMidCutoff, it) },
                            valueRange = 1500f..8000f,
                            colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow),
                            modifier = Modifier.weight(1f)
                        )
                        Text("${settings.crossMidHighCutoff.roundToInt()} Hz", color = theme.primaryGlow, fontSize = 9.sp, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
                    }
                }

                Divider(color = theme.textSec.copy(0.15f))

                // Parallel Crossover output vertical faders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val outputs = listOf(
                        Quad("SUB OUT", settings.crossSubOut, subP) { v: Float -> viewModel.updateCrossoverGains(v, settings.crossLowOut, settings.crossMidOut, settings.crossHighOut) },
                        Quad("LOW OUT", settings.crossLowOut, lowP) { v: Float -> viewModel.updateCrossoverGains(settings.crossSubOut, v, settings.crossMidOut, settings.crossHighOut) },
                        Quad("MID OUT", settings.crossMidOut, midP) { v: Float -> viewModel.updateCrossoverGains(settings.crossSubOut, settings.crossLowOut, v, settings.crossHighOut) },
                        Quad("HIGH OUT", settings.crossHighOut, highP) { v: Float -> viewModel.updateCrossoverGains(settings.crossSubOut, settings.crossLowOut, settings.crossMidOut, v) }
                    )

                    outputs.forEach { (label, gain, rms, onValueChange) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = theme.textSec, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                            // RMS Indicator bar background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(theme.bgMain)
                            ) {
                                val animRmsByFactor = animateFloatAsState(targetValue = rms.coerceIn(0f, 1f), label = "crossover-rms")
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animRmsByFactor.value)
                                        .background(theme.primaryGlow)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .width(16.dp)
                            ) {
                                Slider(
                                    value = gain,
                                    onValueChange = onValueChange,
                                    colors = SliderDefaults.colors(
                                        thumbColor = theme.primaryGlow,
                                        activeTrackColor = theme.primaryGlow,
                                        inactiveTrackColor = theme.bgMain
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .height(95.dp)
                                        .graphicsLayer {
                                            rotationZ = 270f
                                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                        }
                                )
                            }

                            Text("${(gain * 100).roundToInt()}%", color = theme.textMain, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Compressor Card Widget
        Card(
            colors = CardDefaults.cardColors(containerColor = theme.bgCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("STUDIO COMPRESSOR", color = theme.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Manages dynamic sound ranges, tightening kick bass transients", color = theme.textSec, fontSize = 8.sp)
                    }

                    BypassTag(
                        bypassed = viewModel.audioEngine.compressorBypass,
                        onClick = { viewModel.setBypassState("COMPRESSOR", !viewModel.audioEngine.crossoverBypass) },
                        theme = theme
                    )
                }

                // Threshold slider
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("THRESHOLD", color = theme.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${settings.compThreshold.roundToInt()} dB", color = theme.primaryGlow, fontSize = 9.sp)
                    }
                    Slider(
                        value = settings.compThreshold,
                        onValueChange = { viewModel.updateCompressorConfig(thresh = it) },
                        valueRange = -60f..0f,
                        colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow)
                    )
                }

                // Ratio slider
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("RATIO", color = theme.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format("%.1f", settings.compRatio)}:1", color = theme.primaryGlow, fontSize = 9.sp)
                    }
                    Slider(
                        value = settings.compRatio,
                        onValueChange = { viewModel.updateCompressorConfig(ratio = it) },
                        valueRange = 1f..16f,
                        colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow)
                    )
                }

                // Attack and Release parameters in Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("ATTACK", color = theme.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${settings.compAttack.roundToInt()} ms", color = theme.primaryGlow, fontSize = 9.sp)
                        }
                        Slider(
                            value = settings.compAttack,
                            onValueChange = { viewModel.updateCompressorConfig(attack = it) },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("RELEASE", color = theme.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("${settings.compRelease.roundToInt()} ms", color = theme.primaryGlow, fontSize = 9.sp)
                        }
                        Slider(
                            value = settings.compRelease,
                            onValueChange = { viewModel.updateCompressorConfig(release = it) },
                            valueRange = 10f..1000f,
                            colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow)
                        )
                    }
                }
            }
        }

        // Stereo widener and Limiter module combined in Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stereo Imager card
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.bgCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(145.dp)
                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D ST. WIDE", color = theme.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        BypassTag(
                            bypassed = viewModel.audioEngine.widenerBypass,
                            onClick = { viewModel.setBypassState("WIDENER", !viewModel.audioEngine.widenerBypass) },
                            theme = theme
                        )
                    }
                    
                    Column {
                        Text(
                            "WIDTH FACTOR",
                            color = theme.textSec,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = settings.wideFactor,
                                onValueChange = { viewModel.updateStereoImager(factor = it) },
                                valueRange = 0.5f..2.5f,
                                colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${String.format("%.1f", settings.wideFactor)}x",
                                color = theme.primaryGlow,
                                fontSize = 9.sp,
                                modifier = Modifier.width(28.dp),
                                textAlign = TextAlign.End,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Text("Improves ambient soundstage widening fields", color = theme.textSec, fontSize = 8.sp)
                }
            }

            // Hard Peak Limiter card
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.bgCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(145.dp)
                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PEAK LIMITER", color = theme.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        BypassTag(
                            bypassed = viewModel.audioEngine.limiterBypass,
                            onClick = { viewModel.setBypassState("LIMITER", !viewModel.audioEngine.limiterBypass) },
                            theme = theme
                        )
                    }
                    
                    Column {
                        Text(
                            "CEILING GAIN",
                            color = theme.textSec,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = settings.limitCeiling,
                                onValueChange = { viewModel.updateLimiterConfig(ceiling = it) },
                                valueRange = -10f..0f,
                                colors = SliderDefaults.colors(thumbColor = theme.primaryGlow, activeTrackColor = theme.primaryGlow),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${settings.limitCeiling.roundToInt()} dB",
                                color = theme.primaryGlow,
                                fontSize = 9.sp,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Text("Prevents clipping and analog signal distortions", color = theme.textSec, fontSize = 8.sp)
                }
            }
        }
    }
}

// ======================= Tab 3: Routing Matrix =======================
@Composable
fun RoutingMatrixTab(
    viewModel: AudioViewModel,
    routing: RoutingEntity,
    theme: StudioThemeColors
) {
    val items = routing.chainOrder.split(",").filter { it.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "FX SIGNAL CHAIN INTERCEPT ROUTING",
                color = theme.primaryGlow,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Arrange DSP processing order sequentially to drastically alter tonal dynamics.",
                color = theme.textSec,
                fontSize = 9.sp
            )
        }

        Divider(color = theme.textSec.copy(0.15f))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEachIndexed { index, name ->
                val bypassed = when (name) {
                    "EQ" -> routing.eqBypass
                    "CROSSOVER" -> routing.crossoverBypass
                    "COMPRESSOR" -> routing.compressorBypass
                    "WIDENER" -> routing.widenerBypass
                    "LIMITER" -> routing.limiterBypass
                    else -> false
                }

                val flowDescriptor = when (name) {
                    "EQ" -> "Cascaded Multi-Band Peaking Parameter Parametric Filters"
                    "CROSSOVER" -> "4-Way Linkwitz-Riley crossover filter split"
                    "COMPRESSOR" -> "Envelope feed-forward peak dynamic level squasher"
                    "WIDENER" -> "Mid-side phase dynamic stereo stage widener"
                    "LIMITER" -> "Brickwall zero-clipping saturation clipper"
                    else -> ""
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = theme.bgCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Signal order label index
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (bypassed) theme.bgMain else theme.primaryGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (bypassed) theme.textSec else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = when (name) {
                                        "EQ" -> "GRAPHIC EQUALIZER (EQ)"
                                        "CROSSOVER" -> "ACTIVE 4-WAY CROSSOVER"
                                        "COMPRESSOR" -> "DYNAMIC COMPRESSOR"
                                        "WIDENER" -> "STEREO HAAS WIDENER"
                                        "LIMITER" -> "BRICKWALL SAT. LIMITER"
                                        else -> name
                                    },
                                    color = if (bypassed) theme.textSec else theme.textMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = flowDescriptor,
                                    color = theme.textSec,
                                    fontSize = 7.sp
                                )
                            }
                        }

                        // Shift controls and bypass state in Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Shift Up button
                            IconButton(
                                onClick = { viewModel.moveEffectUp(name) },
                                enabled = index > 0,
                                modifier = Modifier.size(30.dp),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = theme.textMain)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                            }

                            // Shift Down button
                            IconButton(
                                onClick = { viewModel.moveEffectDown(name) },
                                enabled = index < items.size - 1,
                                modifier = Modifier.size(30.dp),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = theme.textMain)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                            }

                            // Bypass toggle button
                            IconButton(
                                onClick = { viewModel.setBypassState(name, !bypassed) },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (bypassed) theme.bgMain else theme.primaryGlow.copy(alpha = 0.2f),
                                    contentColor = if (bypassed) theme.textSec else theme.primaryGlow
                                )
                            ) {
                                Icon(
                                    imageVector = if (bypassed) Icons.Default.Block else Icons.Default.PowerSettingsNew,
                                    contentDescription = "Bypass block state",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
    }
}

// Compact helper components
@Composable
fun BypassTag(
    bypassed: Boolean,
    onClick: () -> Unit,
    theme: StudioThemeColors
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (bypassed) theme.bgMain else theme.primaryGlow.copy(alpha = 0.2f),
        modifier = Modifier
            .clickable { onClick() }
            .border(1.dp, if (bypassed) theme.textSec.copy(0.2f) else theme.primaryGlow, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = if (bypassed) "BYPASS" else "ACTIVE",
            color = if (bypassed) theme.textSec else theme.primaryGlow,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontFamily = FontFamily.Monospace
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
