package com.holdup.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BreathingOrb(
    totalSeconds: Int = 8,
    onCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    fun triggerHapticPulse(durationMs: Long = 40) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    // Determine breath cycle duration so that totalSeconds divides evenly into whole cycles
    val cycleDurationSec = when (totalSeconds) {
        5 -> 5f
        8 -> 8f
        12 -> 6f
        16 -> 8f
        else -> if (totalSeconds <= 6) totalSeconds.toFloat() else 8f
    }

    // Master continuous progress from 0.0f to 1.0f over totalSeconds
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(totalSeconds) {
        triggerHapticPulse(50)
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = totalSeconds * 1000,
                easing = LinearEasing
            )
        )
        triggerHapticPulse(60)
        onCompleted()
    }

    val progressValue = animProgress.value
    val totalMs = (totalSeconds * 1000).toFloat()
    val elapsedMs = progressValue * totalMs
    val cycleMs = cycleDurationSec * 1000f
    val cycleProgress = (elapsedMs % cycleMs) / cycleMs

    // Inhale during first half (0.0 to 0.5), Exhale during second half (0.5 to 1.0)
    val isInhale = cycleProgress < 0.5f
    val phaseText = if (isInhale) "Breathe in" else "Breathe out"

    // Haptic pulse when switching breath direction
    var lastIsInhale by remember { mutableStateOf(isInhale) }
    LaunchedEffect(isInhale) {
        if (lastIsInhale != isInhale && progressValue > 0.05f && progressValue < 0.95f) {
            triggerHapticPulse(40)
        }
        lastIsInhale = isInhale
    }

    // Calculate silky sinusoidal scale (0.62f min to 1.0f max)
    val minScale = 0.62f
    val maxScale = 1.0f
    val currentScale = if (isInhale) {
        val u = (cycleProgress / 0.5f).coerceIn(0f, 1f)
        val eased = ((1.0 - cos(u * PI)) / 2.0).toFloat()
        minScale + (maxScale - minScale) * eased
    } else {
        val v = ((cycleProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val eased = ((1.0 - cos(v * PI)) / 2.0).toFloat()
        maxScale - (maxScale - minScale) * eased
    }

    // Smooth continuous spinner arc
    val sweepAngle = progressValue * 360f
    val remainingSeconds = ceil((1f - progressValue) * totalSeconds).toInt().coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.68f
                val ringRadius = baseRadius * 1.25f

                // 1. Ambient outer bloom pulsating with breath
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f * currentScale),
                            secondaryColor.copy(alpha = 0.08f * currentScale),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * currentScale * 1.45f
                    ),
                    radius = baseRadius * currentScale * 1.45f,
                    center = center
                )

                // 2. Delicate track ring (background of spinner)
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 3.5.dp.toPx())
                )

                // 3. Smooth active spinner arc
                if (sweepAngle > 0.5f) {
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Luminous indicator head on the progress arc
                    val headAngleRad = Math.toRadians((-90.0 + sweepAngle))
                    val headX = center.x + ringRadius * cos(headAngleRad).toFloat()
                    val headY = center.y + ringRadius * sin(headAngleRad).toFloat()

                    drawCircle(
                        color = primaryColor.copy(alpha = 0.45f),
                        radius = 6.dp.toPx(),
                        center = Offset(headX, headY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(headX, headY)
                    )
                }

                // 4. Subtle secondary aura ripple
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.30f * currentScale),
                            secondaryColor.copy(alpha = 0.12f * currentScale),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * currentScale * 1.10f
                    ),
                    radius = baseRadius * currentScale * 1.10f,
                    center = center
                )

                // 5. Core luminous breathing orb
                val orbRadius = baseRadius * currentScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.85f),
                            secondaryColor.copy(alpha = 0.60f),
                            primaryColor.copy(alpha = 0.40f)
                        ),
                        center = center,
                        radius = orbRadius
                    ),
                    radius = orbRadius,
                    center = center
                )

                // 6. Gentle 3D spherical specular highlight
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(center.x, center.y - orbRadius * 0.35f),
                        radius = orbRadius * 0.70f
                    ),
                    radius = orbRadius * 0.70f,
                    center = Offset(center.x, center.y - orbRadius * 0.35f)
                )
            }

            // Center Text & Countdown
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = phaseText,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(350))
                    },
                    label = "PhaseTransition"
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "${remainingSeconds}s",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Follow the rhythm • Reset your intention",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 0.2.sp
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
