package com.holdup.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DelayCountdownBar(
    totalSeconds: Int = 10,
    onCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        onCompleted()
    }

    val progress by animateFloatAsState(
        targetValue = 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat()),
        animationSpec = tween(durationMillis = 950, easing = LinearEasing),
        label = "ProgressAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "$remainingSeconds",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Pause for a moment. Notice what you feel right now.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
