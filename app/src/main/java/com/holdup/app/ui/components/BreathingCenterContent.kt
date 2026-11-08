package com.holdup.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BreathPhase(val title: String, val subtitle: String) {
    INHALE("Breathe in", "Expand gently"),
    HOLD("Hold", "Feel the stillness"),
    EXHALE("Breathe out", "Release all tension")
}

@Composable
fun BreathingCenterContent(
    isFinished: Boolean,
    isReady: Boolean,
    phase: BreathPhase,
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isFinished) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Centered",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 23.sp,
                        letterSpacing = (-0.2).sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Mindful pause complete",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        letterSpacing = 0.4.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        } else {
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    fadeIn(tween(350)) togetherWith fadeOut(tween(350))
                },
                label = "PhaseTransition"
            ) { targetPhase ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (!isReady) "Breathe in" else targetPhase.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 23.sp,
                            letterSpacing = (-0.2).sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (!isReady) "Centering..." else targetPhase.subtitle,
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
}
