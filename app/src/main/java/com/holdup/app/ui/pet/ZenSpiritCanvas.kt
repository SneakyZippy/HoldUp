package com.holdup.app.ui.pet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.PetInstance
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetSpecies
import com.holdup.app.data.model.PetStage
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZenSpiritCanvas(
    pet: PetInstance?,
    mood: PetMood,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onPat: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val species = pet?.species ?: PetSpecies.MOSS_TURTLE
    val stage = pet?.stage ?: PetStage.EGG
    val primary = Color(species.primaryColorHex)
    val secondary = Color(species.secondaryColorHex)

    var patCount by remember { mutableIntStateOf(0) }
    var showHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(patCount) {
        if (patCount > 0) {
            showHeartBurst = true
            delay(750L)
            showHeartBurst = false
        }
    }

    var blinkOpen by remember { mutableStateOf(true) }
    LaunchedEffect(mood) {
        while (true) {
            delay(3200L)
            if (mood == PetMood.CONTENT) {
                blinkOpen = false
                delay(150L)
                blinkOpen = true
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "ZenSpiritMotion")
    val breathScale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == PetMood.SAD) 3200 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breath"
    )
    val floatOffset by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == PetMood.SAD) 3600 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Float"
    )
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Orbit"
    )
    val patScale by animateFloatAsState(
        targetValue = if (showHeartBurst) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f),
        label = "PatBounce"
    )

    Box(
        modifier = modifier.then(
            if (onPat != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    patCount++
                    onPat()
                }
            } else {
                Modifier
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseRadius = (size.minDimension * 0.34f) * breathScale * patScale
            val bobY = if (mood == PetMood.SAD) 2f else floatOffset * (size.minDimension * 0.03f)
            val spiritCenter = Offset(size.width / 2f, size.height / 2f + bobY)
            val auraAlpha = when (mood) {
                PetMood.RADIANT -> 0.32f
                PetMood.CONTENT -> 0.18f
                PetMood.SAD -> 0.08f
            }

            // Ambient glowing aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = auraAlpha), secondary.copy(alpha = auraAlpha * 0.4f), Color.Transparent),
                    center = spiritCenter,
                    radius = baseRadius * 1.85f
                ),
                radius = baseRadius * 1.85f,
                center = spiritCenter
            )

            // Orbiting stardust particles when not compact and not sad
            if (!isCompact && mood != PetMood.SAD) {
                val particleCount = if (mood == PetMood.RADIANT) 6 else 3
                val radBase = Math.toRadians(orbitAngle.toDouble())
                for (i in 0 until particleCount) {
                    val angle = radBase + (i * 2.0 * Math.PI / particleCount)
                    val dist = baseRadius * 1.42f
                    val px = spiritCenter.x + (dist * cos(angle)).toFloat()
                    val py = spiritCenter.y + (dist * sin(angle)).toFloat()
                    drawCircle(
                        color = (if (i % 2 == 0) primary else secondary).copy(alpha = 0.65f),
                        radius = if (mood == PetMood.RADIANT) 3.5.dp.toPx() else 2.2.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }

            drawZenSpirit(
                species = species,
                stage = stage,
                mood = mood,
                center = spiritCenter,
                radius = baseRadius,
                primary = if (mood == PetMood.SAD) primary.copy(alpha = 0.72f) else primary,
                secondary = if (mood == PetMood.SAD) secondary.copy(alpha = 0.65f) else secondary,
                eggProgress = pet?.stageProgressFraction ?: 0f,
                wobbleDeg = floatOffset * 4f,
                blinkOpen = blinkOpen
            )
        }

        if (showHeartBurst && !isCompact) {
            Text(
                text = "✨💖✨",
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp)
            )
        }
    }
}
