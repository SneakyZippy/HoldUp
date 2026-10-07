package com.holdup.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

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

    val infiniteTransition = rememberInfiniteTransition(label = "BreathingCycle")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    val ambientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing)
        ),
        label = "AmbientRotation"
    )

    LaunchedEffect(Unit) {
        triggerHapticPulse(50)
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            val cycleSec = (totalSeconds - remainingSeconds) % 8
            when (cycleSec) {
                0, 1, 2, 3 -> {
                    if (phase != "Breathe In") {
                        phase = "Breathe In"
                        triggerHapticPulse(45)
                    }
                }
                4 -> {
                    if (phase != "Hold") {
                        phase = "Hold"
                        triggerHapticPulse(25)
                    }
                }
                else -> {
                    if (phase != "Breathe Out") {
                        phase = "Breathe Out"
                        triggerHapticPulse(45)
                    }
                }
            }
        }
        triggerHapticPulse(75)
        onCompleted()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.82f

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f * scale),
                            secondaryColor.copy(alpha = 0.15f * scale),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.3f
                    ),
                    radius = baseRadius * 1.3f,
                    center = center
                )

                // Middle pulsing aura ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.5f),
                            secondaryColor.copy(alpha = 0.25f)
                        ),
                        center = center,
                        radius = baseRadius * scale
                    ),
                    radius = baseRadius * scale,
                    center = center
                )

                // Outer thin decorative progress arc
                val sweepProgress = ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()) * 360f
                drawArc(
                    color = primaryColor.copy(alpha = 0.4f),
                    startAngle = -90f,
                    sweepAngle = sweepProgress,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Core luminous sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.9f),
                            secondaryColor.copy(alpha = 0.75f)
                        ),
                        center = center,
                        radius = baseRadius * scale * 0.85f
                    ),
                    radius = baseRadius * scale * 0.85f,
                    center = center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = phase,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "${remainingSeconds}s",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Follow the rhythm. Reset your intention.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
