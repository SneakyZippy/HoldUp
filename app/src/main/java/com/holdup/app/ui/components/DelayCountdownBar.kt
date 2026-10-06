package com.holdup.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.ui.theme.ZenBorder
import com.holdup.app.ui.theme.ZenSage
import com.holdup.app.ui.theme.ZenTextPrimary
import com.holdup.app.ui.theme.ZenTextSecondary
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
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$remainingSeconds",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = ZenSage
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ZenBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .background(ZenSage)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Take a pause. Notice what you feel right now.",
            style = MaterialTheme.typography.bodyMedium.copy(color = ZenTextSecondary)
        )
    }
}
