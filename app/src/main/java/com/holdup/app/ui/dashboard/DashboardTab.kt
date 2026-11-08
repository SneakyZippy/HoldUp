package com.holdup.app.ui.dashboard

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.holdup.app.data.model.DailyStats

@Composable
fun DashboardTab(
    isMonitoringEnabled: Boolean,
    isChildSafetyEnabled: Boolean,
    onToggleMonitoring: (Boolean) -> Unit,
    todayStats: DailyStats,
    monitoredPackages: Set<String>,
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenTour: () -> Unit
) {
    val context = LocalContext.current

    val installedShieldedPackages = remember(monitoredPackages) {
        val pm = context.packageManager
        monitoredPackages.filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardHeader(
                isMonitoringEnabled = isMonitoringEnabled,
                isChildSafetyEnabled = isChildSafetyEnabled,
                onToggleMonitoring = onToggleMonitoring,
                onOpenTour = onOpenTour
            )
        }

        if (!isAccessibilityGranted || !isUsageAccessGranted) {
            item {
                PermissionSetupAlert(
                    isAccessibilityGranted = isAccessibilityGranted,
                    isUsageAccessGranted = isUsageAccessGranted,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenUsageSettings = onOpenUsageSettings
                )
            }
        }

        item {
            MindfulIntentionCard(
                todayStats = todayStats,
                shieldedAppsCount = installedShieldedPackages.size
            )
        }

        item {
            BentoStatsRow(todayStats = todayStats)
        }

        if (installedShieldedPackages.isNotEmpty()) {
            item {
                ActiveShieldsRow(shieldedPackages = installedShieldedPackages)
            }
        }

        item {
            DailyMindfulTipCard()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionSetupAlert(
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Setup Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "To pause doomscrolling when apps open, HoldUp needs accessibility and usage access permissions enabled.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isAccessibilityGranted) {
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Enable Interceptor", fontWeight = FontWeight.SemiBold)
                    }
                }
                if (!isUsageAccessGranted) {
                    OutlinedButton(
                        onClick = onOpenUsageSettings,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                    ) {
                        Text("Usage Access", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
