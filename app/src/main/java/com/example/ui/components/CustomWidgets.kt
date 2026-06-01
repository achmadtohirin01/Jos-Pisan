package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10
import kotlin.math.max

// Exposes Visual Theme Colors dynamically
data class StudioThemeColors(
    val bgMain: Color,
    val bgCard: Color,
    val bgHeader: Color,
    val bgVisualizer: Color,
    val primaryGlow: Color,
    val secondaryAccent: Color,
    val textMain: Color,
    val textSec: Color,
    val faderGlow: Color,
    val sliderBrush: Brush,
    val visualizerGradient: Brush
)

object ThemeRegistry {
    val Metallic = StudioThemeColors(
        bgMain = Color(0xFF1E2124),
        bgCard = Color(0xFF2E3236),
        bgHeader = Color(0xFF181B1D),
        bgVisualizer = Color(0xFF111214),
        primaryGlow = Color(0xFF00FFCC),
        secondaryAccent = Color(0xFFFF9900),
        textMain = Color(0xFFECEFF1),
        textSec = Color(0xFF90A4AE),
        faderGlow = Color(0xFF00C8FF),
        sliderBrush = Brush.verticalGradient(listOf(Color(0xFFB0BEC5), Color(0xFF181B1D))),
        visualizerGradient = Brush.verticalGradient(listOf(Color(0xFF00FFCC), Color(0xFF111214)))
    )

    val GlowInTheDark = StudioThemeColors(
        bgMain = Color(0xFF0A0A0B),
        bgCard = Color(0xFF16161A),
        bgHeader = Color(0xFF121214),
        bgVisualizer = Color(0xFF050505),
        primaryGlow = Color(0xFF00F0FF),
        secondaryAccent = Color(0xFF10B981),
        textMain = Color(0xFFE2E8F0),
        textSec = Color(0xFF64748B),
        faderGlow = Color(0xFF00F0FF),
        sliderBrush = Brush.verticalGradient(listOf(Color(0xFF00F0FF), Color(0xFF121214))),
        visualizerGradient = Brush.verticalGradient(listOf(Color(0xFF00F0FF), Color(0xFF050505)))
    )

    val BlueGreen = StudioThemeColors(
        bgMain = Color(0xFF081C1C),
        bgCard = Color(0xFF0E2C2C),
        bgHeader = Color(0xFF061414),
        bgVisualizer = Color(0xFF030A0A),
        primaryGlow = Color(0xFF00F0FF),
        secondaryAccent = Color(0xFF00FF88),
        textMain = Color(0xFFE2F3F3),
        textSec = Color(0xFF638C8C),
        faderGlow = Color(0xFF00F0FF),
        sliderBrush = Brush.verticalGradient(listOf(Color(0xFF00F0FF), Color(0xFF061414))),
        visualizerGradient = Brush.verticalGradient(listOf(Color(0xFF00F0FF), Color(0xFF00FF88)))
    )

    val PurpleNeon = StudioThemeColors(
        bgMain = Color(0xFF110B22),
        bgCard = Color(0xFF1D1435),
        bgHeader = Color(0xFF0D081F),
        bgVisualizer = Color(0xFF060312),
        primaryGlow = Color(0xFFD600FF),
        secondaryAccent = Color(0xFFFFE600),
        textMain = Color(0xFFF7F4FA),
        textSec = Color(0xFF8B7AA3),
        faderGlow = Color(0xFFE000FF),
        sliderBrush = Brush.verticalGradient(listOf(Color(0xFFD600FF), Color(0xFF0D081F))),
        visualizerGradient = Brush.verticalGradient(listOf(Color(0xFFD600FF), Color(0xFF060312)))
    )

    fun get(name: String): StudioThemeColors {
        return when (name) {
            "Metallic" -> Metallic
            "Glow in the Dark" -> GlowInTheDark
            "Blue Green" -> BlueGreen
            "Purple Neon" -> PurpleNeon
            else -> GlowInTheDark
        }
    }
}

// 1. Dual VU Meter L & R
@Composable
fun DualVuMeter(
    levelL: Float,
    levelR: Float,
    theme: StudioThemeColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bgCard)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("VU L", color = theme.textMain, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("VU R", color = theme.textMain, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VuBar(level = levelL, theme = theme, modifier = Modifier.weight(1f))
            VuBar(level = levelR, theme = theme, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun VuBar(
    level: Float,
    theme: StudioThemeColors,
    modifier: Modifier = Modifier
) {
    // Transform linear envelope (0f to 1f) safely to dB Scale for VU meters
    val animatedLevel = remember { Animatable(0f) }
    LaunchedEffect(level) {
        animatedLevel.animateTo(level.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessHigh))
    }

    Canvas(modifier = modifier.fillMaxHeight()) {
        val totalSegments = 16
        val gap = 2.dp.toPx()
        val segHeight = (size.height - (totalSegments - 1) * gap) / totalSegments

        for (i in 0 until totalSegments) {
            val idxFromBottom = totalSegments - 1 - i
            val limit = idxFromBottom.toFloat() / totalSegments
            val isActive = animatedLevel.value >= limit

            // Dynamic color bands: Green -> Yellow -> Red
            val color = when {
                i < totalSegments * 0.2f -> Color(0xFFFF2255) // Top Red
                i < totalSegments * 0.45f -> Color(0xFFFFBB00) // Mid Yellow
                else -> Color(0xFF00FF66) // Lower Green
            }

            val finalColor = if (isActive) color else color.copy(0.15f)
            
            drawRect(
                color = finalColor,
                topLeft = Offset(0f, i * (segHeight + gap)),
                size = Size(size.width, segHeight)
            )
        }
    }
}

// 2. Real-time Spektrum FFT simulation
@Composable
fun FftSpectrumVisualizer(
    spectrum: FloatArray,
    theme: StudioThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bgVisualizer)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        // Overlay info text block (from Immersive UI HTML design specifications)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp, start = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "ENGINE",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textSec.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "OBOE NATIVE 32B",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryGlow
                )
            }
            Column {
                Text(
                    text = "LATENCY",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textSec.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "7.2ms",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = theme.secondaryAccent
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = spectrum.size
            val gap = 3.dp.toPx()
            val totalGapWidth = gap * (barCount - 1)
            val barWidth = (size.width - totalGapWidth) / barCount

            for (i in 0 until barCount) {
                val amp = spectrum[i].coerceIn(0.01f, 1f)
                val barHeight = size.height * amp
                val x = i * (barWidth + gap)
                val y = size.height - barHeight

                // Draw solid equalizer bar
                drawRect(
                    brush = theme.visualizerGradient,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Optional glow accents to match themes
                if (theme.primaryGlow != Color.Transparent) {
                    drawRect(
                        color = theme.primaryGlow.copy(alpha = 0.3f),
                        topLeft = Offset(x - 1f, y - 1f),
                        size = Size(barWidth + 2f, barHeight + 2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

// 3. Stylized Crossover Curves drawing
@Composable
fun CrossoverCurveVisualizer(
    subOut: Float,
    lowOut: Float,
    midOut: Float,
    highOut: Float,
    subLowCut: Float,
    lowMidCut: Float,
    midHighCut: Float,
    theme: StudioThemeColors,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bgCard)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Let's draw ideal 4-way filter response curves
        // Convert cuts (20Hz to 20kHz logarithmic) to coordinates
        fun hzToX(f: Double): Float {
            val minF = 20.0
            val maxF = 20000.0
            val logMin = log10(minF)
            val logMax = log10(maxF)
            val pct = (log10(f.coerceIn(minF, maxF)) - logMin) / (logMax - logMin)
            return (pct * w).toFloat()
        }

        val xSubLow = hzToX(subLowCut.toDouble())
        val xLowMid = hzToX(lowMidCut.toDouble())
        val xMidHigh = hzToX(midHighCut.toDouble())

        // Subband path (Cyan/Blue)
        val subColor = Color(0xFF00FFCC).copy(0.2f + 0.8f * subOut)
        // Lowband path (Green)
        val lowColor = Color(0xFF55FF33).copy(0.2f + 0.8f * lowOut)
        // Midband path (Yellow/Orange)
        val midColor = Color(0xFFFFB300).copy(0.2f + 0.8f * midOut)
        // Highband path (Pink/Red)
        val highColor = Color(0xFFFF1155).copy(0.2f + 0.8f * highOut)

        // Draw Sub curve
        drawFilterCurve(
            type = 1, // Lowpass
            cutX = xSubLow,
            gain = subOut,
            color = subColor,
            width = w,
            height = h
        )

        // Draw Low Band curve (Bandpass)
        drawFilterCurve(
            type = 3, // Bandpass
            cutX = xSubLow,
            cutX2 = xLowMid,
            gain = lowOut,
            color = lowColor,
            width = w,
            height = h
        )

        // Draw Mid Band curve (Bandpass)
        drawFilterCurve(
            type = 3, // Bandpass
            cutX = xLowMid,
            cutX2 = xMidHigh,
            gain = midOut,
            color = midColor,
            width = w,
            height = h
        )

        // Draw High curve
        drawFilterCurve(
            type = 2, // Highpass
            cutX = xMidHigh,
            gain = highOut,
            color = highColor,
            width = w,
            height = h
        )

        // Draw cutoff indicator lines
        drawLine(
            color = theme.textSec.copy(0.4f),
            start = Offset(xSubLow, 0f),
            end = Offset(xSubLow, h),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = theme.textSec.copy(0.4f),
            start = Offset(xLowMid, 0f),
            end = Offset(xLowMid, h),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = theme.textSec.copy(0.4f),
            start = Offset(xMidHigh, 0f),
            end = Offset(xMidHigh, h),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFilterCurve(
    type: Int, // 1 lowpass, 2 highpass, 3 bandpass
    cutX: Float,
    cutX2: Float = 0f,
    gain: Float,
    color: Color,
    width: Float,
    height: Float
) {
    val curvePath = androidx.compose.ui.graphics.Path()
    val peakHeight = height * 0.15f + (height * 0.7f * gain)
    val baseline = height - 10f

    curvePath.moveTo(0f, baseline)

    when (type) {
        1 -> { // Lowpass response
            curvePath.lineTo(0f, height - peakHeight)
            var currentX = 0f
            while (currentX < width) {
                val y = if (currentX <= cutX) {
                    height - peakHeight
                } else {
                    val slope = (currentX - cutX) / (width - cutX)
                    max(height - peakHeight + (slope * peakHeight), height - 10f)
                }
                curvePath.lineTo(currentX, y)
                currentX += 5f
            }
        }
        2 -> { // Highpass response
            var currentX = 0f
            while (currentX < width) {
                val y = if (currentX >= cutX) {
                    height - peakHeight
                } else {
                    val slope = (cutX - currentX) / cutX
                    max(height - peakHeight + (slope * peakHeight), height - 10f)
                }
                curvePath.lineTo(currentX, y)
                currentX += 5f
            }
        }
        3 -> { // Bandpass curve response
            var currentX = 0f
            while (currentX < width) {
                val y = when {
                    currentX in cutX..cutX2 -> {
                        height - peakHeight
                    }
                    currentX < cutX -> {
                        val slope = (cutX - currentX) / cutX
                        max(height - peakHeight + (slope * peakHeight), height - 10f)
                    }
                    else -> {
                        val slope = (currentX - cutX2) / (width - cutX2)
                        max(height - peakHeight + (slope * peakHeight), height - 10f)
                    }
                }
                curvePath.lineTo(currentX, y)
                currentX += 5f
            }
        }
    }
    curvePath.lineTo(width, baseline)
    curvePath.close()

    drawPath(curvePath, color = color)
}
