package com.holdup.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.DailyStats
import com.holdup.app.ui.components.zenGlassBorder
import com.holdup.app.ui.theme.LocalAccentPreset

@Composable
fun BentoStatsRow(
    todayStats: DailyStats,
    onPointsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CleanFocusTile(
            todayStats = todayStats,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        MindfulPointsTile(
            todayStats = todayStats,
            onClick = onPointsClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun ZenGradientProgressBar(
    progress: Float,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(colors))
            )
        }
    }
}

@Composable
private fun CleanFocusTile(
    todayStats: DailyStats,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccentPreset.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = zenGlassBorder(),
        modifier = modifier.heightIn(min = 160.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (todayStats.dailyGraceBudgetMinutes > 0) {
                            "${todayStats.graceMinutesRemaining}m grace"
                        } else {
                            "Strict ${todayStats.gracePreset.multiplierLabel}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Clean Focus",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = if (todayStats.cleanFocusMinutesToday >= 60) {
                        "${todayStats.currentCleanStreakHours}h ${todayStats.currentCleanStreakMinutesPart}m"
                    } else {
                        "${todayStats.cleanFocusMinutesToday}m streak"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Column {
                ZenGradientProgressBar(
                    progress = todayStats.focusHourProgress,
                    colors = accent.gradientColors
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${todayStats.minutesToNextFocusPoint}m to +${todayStats.gracePreset.hourlyPoints} pts",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun MindfulPointsTile(
    todayStats: DailyStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prevTierTarget = when (todayStats.currentLevelNumber) {
        1 -> 0
        2 -> 100
        3 -> 300
        4 -> 600
        5 -> 1000
        else -> 0
    }
    val targetDelta = (todayStats.nextLevelTarget - prevTierTarget).coerceAtLeast(1)
    val currentProgress = ((todayStats.lifetimePoints - prevTierTarget).toFloat() / targetDelta.toFloat())
        .coerceIn(0f, 1f)

    val accent = LocalAccentPreset.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = zenGlassBorder(),
        modifier = modifier
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (todayStats.currentStreakDays > 0) {
                            "${todayStats.currentStreakDays}d streak"
                        } else {
                            "+${todayStats.pointsEarnedToday} today"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "${todayStats.userLevel} • Tier ${todayStats.currentLevelNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${todayStats.spendablePoints} pts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1
                )
            }

            Column {
                ZenGradientProgressBar(
                    progress = currentProgress,
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                val remaining = (todayStats.nextLevelTarget - todayStats.lifetimePoints).coerceAtLeast(0)
                Text(
                    text = if (remaining > 0) "${todayStats.lifetimePoints} XP • $remaining to Tier ${todayStats.currentLevelNumber + 1}" else "${todayStats.lifetimePoints} XP • Max tier!",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
