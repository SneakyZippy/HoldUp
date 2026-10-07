package com.holdup.app.ui.components

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun RealityCheckHeader(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var appName by remember { mutableStateOf("App") }
    var todayUsageMinutes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(packageName) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {}

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val statsList = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val appStats = statsList?.firstOrNull { it.packageName == packageName }
            if (appStats != null) {
                todayUsageMinutes = appStats.totalTimeInForeground / (1000 * 60)
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon with gentle halo ring
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                packageName = packageName,
                size = 54.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOLD UP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Text(
            text = "Opening $appName",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (todayUsageMinutes > 0) {
                        "Today: ${todayUsageMinutes}m spent here"
                    } else {
                        "Mindful Moment"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
