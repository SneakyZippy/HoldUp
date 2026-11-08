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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    isReady: Boolean = true,
    onCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    fun triggerHaptic(durationMs: Long = 35) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    // Lock session duration once ready so async updates cannot cancel/reset an active breath session
    val sessionSeconds = remember(isReady) { if (isReady) totalSeconds else 8 }

    val cycleDurationSec = when {
        sessionSeconds <= 5 -> sessionSeconds.toFloat()
        sessionSeconds % 8 == 0 -> 8f
        sessionSeconds % 6 == 0 -> 6f
        else -> 8f
    }

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(isReady, sessionSeconds) {
        if (!isReady) return@LaunchedEffect
        triggerHaptic(45)
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = sessionSeconds * 1000,
                easing = LinearEasing
            )
        )
        triggerHaptic(55)
        onCompleted()
    }

    val progressValue = animProgress.value
    val isFinished = progressValue >= 0.999f
    val totalMs = (sessionSeconds * 1000).toFloat()
    val elapsedMs = progressValue * totalMs
    val cycleMs = cycleDurationSec * 1000f
    val cycleProgress = if (cycleMs > 0) (elapsedMs % cycleMs) / cycleMs else 0f

    // 3-Phase Breathwork: Inhale 42%, Hold 16%, Exhale 42%
    val (phase, phaseProgress) = when {
        !isReady -> BreathPhase.INHALE to 0f
        isFinished -> BreathPhase.EXHALE to 1f
        cycleProgress < 0.42f -> BreathPhase.INHALE to (cycleProgress / 0.42f).coerceIn(0f, 1f)
        cycleProgress < 0.58f -> BreathPhase.HOLD to ((cycleProgress - 0.42f) / 0.16f).coerceIn(0f, 1f)
        else -> BreathPhase.EXHALE to ((cycleProgress - 0.58f) / 0.42f).coerceIn(0f, 1f)
    }

    var lastPhase by remember { mutableStateOf(phase) }
    LaunchedEffect(phase) {
        if (isReady && !isFinished && lastPhase != phase && progressValue > 0.03f && progressValue < 0.97f) {
            triggerHaptic(if (phase == BreathPhase.HOLD) 25 else 35)
        }
        lastPhase = phase
    }

    val minScale = 0.62f
    val maxScale = 1.0f
    val currentScale = when {
        !isReady || isFinished -> minScale
        phase == BreathPhase.INHALE -> {
            val eased = ((1.0 - cos(phaseProgress * PI)) / 2.0).toFloat()
            minScale + (maxScale - minScale) * eased
        }
        phase == BreathPhase.HOLD -> {
            val shimmer = sin(phaseProgress * PI * 2).toFloat() * 0.015f
            maxScale + shimmer
        }
        else -> {
            val eased = ((1.0 - cos(phaseProgress * PI)) / 2.0).toFloat()
            maxScale - (maxScale - minScale) * eased
        }
    }

    val rotationDeg = when {
        !isReady || isFinished -> 0f
        phase == BreathPhase.INHALE -> phaseProgress * 24f
        phase == BreathPhase.HOLD -> 24f + phaseProgress * 4f
        else -> 28f - phaseProgress * 28f
    }

    val sweepAngle = if (isFinished) 360f else (progressValue * 360f)
    val remainingSeconds = if (isFinished) 0 else ceil((1f - progressValue) * sessionSeconds).toInt().coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(270.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.65f
                val ringRadius = baseRadius * 1.30f

                // 1. Ambient outer glow bloom
                BreathingBloomRenderer.drawAmbientBloom(
                    scope = this,
                    center = center,
                    baseRadius = baseRadius,
                    scale = currentScale,
                    primary = primaryColor,
                    secondary = secondaryColor
                )

                // 2. Floating stardust particles
                BreathingBloomRenderer.drawFloatingParticles(
                    scope = this,
                    center = center,
                    maxRadius = ringRadius * 0.95f,
                    rotationRad = Math.toRadians(rotationDeg.toDouble()).toFloat(),
                    scale = currentScale,
                    primary = primaryColor
                )

                // 3. Sacred Lotus organic petals (layer 1: outer counter-rotating)
                val petalRadius = baseRadius * currentScale * 0.64f
                val petalSpread = baseRadius * currentScale * 0.52f
                BreathingBloomRenderer.drawLotusPetals(
                    scope = this,
                    center = center,
                    petalRadius = petalRadius,
                    petalSpread = petalSpread,
                    rotationDeg = -rotationDeg * 0.6f,
                    primary = secondaryColor,
                    secondary = primaryColor,
                    petalCount = 6
                )

                // 4. Sacred Lotus organic petals (layer 2: forward rotating)
                BreathingBloomRenderer.drawLotusPetals(
                    scope = this,
                    center = center,
                    petalRadius = petalRadius * 0.90f,
                    petalSpread = petalSpread * 0.78f,
                    rotationDeg = rotationDeg,
                    primary = primaryColor,
                    secondary = secondaryColor,
                    petalCount = 6
                )

                // 5. Central glowing core aura and specular highlight
                val coreRadius = baseRadius * currentScale * 0.48f
                BreathingBloomRenderer.drawCoreAuraAndOrb(
                    scope = this,
                    center = center,
                    coreRadius = coreRadius,
                    primary = primaryColor,
                    secondary = secondaryColor
                )

                // 6. Perimeter progress halo with luminous head bead
                BreathingBloomRenderer.drawProgressHalo(
                    scope = this,
                    center = center,
                    ringRadius = ringRadius,
                    sweepAngle = sweepAngle,
                    primary = primaryColor
                )
            }

            // Center Text & Countdown
            BreathingCenterContent(
                isFinished = isFinished,
                isReady = isReady,
                phase = phase,
                remainingSeconds = remainingSeconds
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Follow the rhythm • Reset your intention",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                letterSpacing = 0.2.sp
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
