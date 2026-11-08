package com.holdup.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.DailyStats
import com.holdup.app.ui.theme.LocalAccentPreset

@Composable
fun MindfulIntentionCard(
    todayStats: DailyStats,
    shieldedAppsCount: Int
) {
    val accent = LocalAccentPreset.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = Color.White.copy(alpha = 0.06f)

    val targetProgress = if (todayStats.totalInterceptions > 0) {
        (todayStats.walkedAwayCount.toFloat() / todayStats.totalInterceptions.toFloat())
    } else {
        1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(1200),
        label = "GaugeProgress"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IntentionHeader(savedMinutes = todayStats.estimatedMinutesSaved)
            Spacer(modifier = Modifier.height(18.dp))

            GaugeDial(
                percentage = todayStats.successRatePercent,
                animatedProgress = animatedProgress,
                primaryColor = primaryColor,
                trackColor = trackColor,
                gradientColors = accent.gradientColors
            )

            Spacer(modifier = Modifier.height(20.dp))

            StatsRow(
                walkedAwayCount = todayStats.walkedAwayCount,
                proceededCount = todayStats.proceededCount,
                shieldedAppsCount = shieldedAppsCount
            )
        }
    }
}

@Composable
private fun IntentionHeader(savedMinutes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TODAY'S INTENTION",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.4.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timelapse,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "~${savedMinutes}m saved",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun GaugeDial(
    percentage: Int,
    animatedProgress: Float,
    primaryColor: Color,
    trackColor: Color,
    gradientColors: List<Color>
) {
    Box(
        modifier = Modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension - strokeWidth) / 2f

            // Soft ambient glow behind percentage
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.9f
                ),
                radius = radius * 0.9f,
                center = center
            )

            // Background circular track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Animated progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        gradientColors.first().copy(alpha = 0.6f),
                        gradientColors.first(),
                        gradientColors.last()
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "Mindful Win Rate",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun StatsRow(
    walkedAwayCount: Int,
    proceededCount: Int,
    shieldedAppsCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(value = "$walkedAwayCount", label = "Walked Away", valueColor = MaterialTheme.colorScheme.primary)
            VerticalDivider(modifier = Modifier.height(24.dp), color = Color.White.copy(alpha = 0.08f))
            StatItem(value = "$proceededCount", label = "Mindful Opens", valueColor = MaterialTheme.colorScheme.onSurface)
            VerticalDivider(modifier = Modifier.height(24.dp), color = Color.White.copy(alpha = 0.08f))
            StatItem(value = "$shieldedAppsCount", label = "Shielded Apps", valueColor = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        )
    }
}
