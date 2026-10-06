package com.holdup.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.ui.theme.ZenLavender
import com.holdup.app.ui.theme.ZenSage
import com.holdup.app.ui.theme.ZenTextSecondary
import kotlinx.coroutines.delay

@Composable
fun BreathingOrb(
    totalSeconds: Int = 8,
    onCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var phase by remember { mutableStateOf("Breathe In") }

    // Haptics helper
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

    // Breathing rhythm: Inhale 4s, Hold 2s, Exhale 4s
    val infiniteTransition = rememberInfiniteTransition(label = "BreathingCycle")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    // Countdown timer & Phase management
    LaunchedEffect(Unit) {
        triggerHapticPulse(60)
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            val cycleSec = (totalSeconds - remainingSeconds) % 8
            when (cycleSec) {
                0, 1, 2, 3 -> {
                    if (phase != "Breathe In") {
                        phase = "Breathe In"
                        triggerHapticPulse(50)
                    }
                }
                4 -> {
                    if (phase != "Hold") {
                        phase = "Hold"
                        triggerHapticPulse(30)
                    }
                }
                else -> {
                    if (phase != "Breathe Out") {
                        phase = "Breathe Out"
                        triggerHapticPulse(50)
                    }
                }
            }
        }
        triggerHapticPulse(80)
        onCompleted()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.85f

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ZenSage.copy(alpha = 0.25f),
                            ZenLavender.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * scale * 1.25f
                    ),
                    radius = baseRadius * scale * 1.25f,
                    center = center
                )

                // Main breathing orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ZenSage.copy(alpha = 0.85f),
                            ZenLavender.copy(alpha = 0.65f)
                        ),
                        center = center,
                        radius = baseRadius * scale
                    ),
                    radius = baseRadius * scale,
                    center = center
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = phase,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${remainingSeconds}s",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Follow the rhythm. Allow yourself a mindful moment.",
            style = MaterialTheme.typography.bodyMedium.copy(color = ZenTextSecondary),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
