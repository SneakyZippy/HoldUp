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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

enum class BreathPhase(val title: String, val subtitle: String) {
    INHALE("Breathe in", "Expand gently"),
    HOLD("Hold", "Feel the stillness"),
    EXHALE("Breathe out", "Release all tension")
}

@Composable
fun BreathingOrb(
    totalSeconds: Int = 8,
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

    val cycleDurationSec = when {
        totalSeconds <= 5 -> totalSeconds.toFloat()
        totalSeconds % 8 == 0 -> 8f
        totalSeconds % 6 == 0 -> 6f
        else -> 8f
    }

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(totalSeconds) {
        triggerHaptic(45)
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = totalSeconds * 1000,
                easing = LinearEasing
            )
        )
        triggerHaptic(55)
        onCompleted()
    }

    val progressValue = animProgress.value
    val totalMs = (totalSeconds * 1000).toFloat()
    val elapsedMs = progressValue * totalMs
    val cycleMs = cycleDurationSec * 1000f
    val cycleProgress = (elapsedMs % cycleMs) / cycleMs

    // 3-Phase Breathwork: Inhale 42%, Hold 16%, Exhale 42%
    val (phase, phaseProgress) = when {
        cycleProgress < 0.42f -> BreathPhase.INHALE to (cycleProgress / 0.42f).coerceIn(0f, 1f)
        cycleProgress < 0.58f -> BreathPhase.HOLD to ((cycleProgress - 0.42f) / 0.16f).coerceIn(0f, 1f)
        else -> BreathPhase.EXHALE to ((cycleProgress - 0.58f) / 0.42f).coerceIn(0f, 1f)
    }

    var lastPhase by remember { mutableStateOf(phase) }
    LaunchedEffect(phase) {
        if (lastPhase != phase && progressValue > 0.03f && progressValue < 0.97f) {
            triggerHaptic(if (phase == BreathPhase.HOLD) 25 else 35)
        }
        lastPhase = phase
    }

    val minScale = 0.62f
    val maxScale = 1.0f
    val currentScale = when (phase) {
        BreathPhase.INHALE -> {
            val eased = ((1.0 - cos(phaseProgress * PI)) / 2.0).toFloat()
            minScale + (maxScale - minScale) * eased
        }
        BreathPhase.HOLD -> {
            // Subtle serene micro-breath pulse
            val shimmer = sin(phaseProgress * PI * 2).toFloat() * 0.015f
            maxScale + shimmer
        }
        BreathPhase.EXHALE -> {
            val eased = ((1.0 - cos(phaseProgress * PI)) / 2.0).toFloat()
            maxScale - (maxScale - minScale) * eased
        }
    }

    val rotationDeg = when (phase) {
        BreathPhase.INHALE -> phaseProgress * 24f
        BreathPhase.HOLD -> 24f + phaseProgress * 4f
        BreathPhase.EXHALE -> 28f - phaseProgress * 28f
    }

    val sweepAngle = progressValue * 360f
    val remainingSeconds = ceil((1f - progressValue) * totalSeconds).toInt().coerceAtLeast(0)

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(350))
                    },
                    label = "PhaseTransition"
                ) { targetPhase ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = targetPhase.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 23.sp,
                                letterSpacing = (-0.2).sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = targetPhase.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                letterSpacing = 0.4.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.38f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Text(
                        text = "${remainingSeconds}s",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
            }
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
