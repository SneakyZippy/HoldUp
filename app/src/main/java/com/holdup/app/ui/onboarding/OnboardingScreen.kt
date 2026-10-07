package com.holdup.app.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.preferences.PreferencesManager
import com.holdup.app.ui.components.AppIcon

@Composable
fun OnboardingScreen(
    monitoredPackages: Set<String>,
    onToggleApp: (String, Boolean) -> Unit,
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentPage by remember { mutableIntStateOf(0) } // 0, 1, 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val isSelected = currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 28.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                    )
                }
            }

            // Body Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (currentPage) {
                    0 -> OnboardingWelcomeSlide()
                    1 -> OnboardingShieldsSlide(
                        monitoredPackages = monitoredPackages,
                        onToggleApp = onToggleApp
                    )
                    2 -> OnboardingPermissionsSlide(
                        isAccessibilityGranted = isAccessibilityGranted,
                        isUsageAccessGranted = isUsageAccessGranted,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenUsageSettings = onOpenUsageSettings
                    )
                }
            }

            // Bottom Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentPage--
                        }
                    ) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentPage < 2) {
                            currentPage++
                        } else {
                            onCompleteOnboarding()
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        text = if (currentPage == 2) "Start HoldUp" else "Continue",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (currentPage == 2) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcomeSlide() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Pause Before You Scroll",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Opening social media is often pure autopilot. HoldUp gives your prefrontal cortex a 5-second breath to decide if it's what you truly want.",
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
        )
    }
}

@Composable
fun OnboardingShieldsSlide(
    monitoredPackages: Set<String>,
    onToggleApp: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Detect target apps installed on device
    val popularDoomscroll = PreferencesManager.DEFAULT_TARGET_PACKAGES
    val installedTargets = remember {
        val pm = context.packageManager
        popularDoomscroll.filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Choose What to Shield",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select apps where you want mindful friction",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(installedTargets) { pkg ->
                val isChecked = monitoredPackages.contains(pkg)
                val appName = remember(pkg) {
                    try {
                        val pm = context.packageManager
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        pkg.substringAfterLast(".")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isChecked) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleApp(pkg, !isChecked)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(packageName = pkg, size = 40.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleApp(pkg, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPermissionsSlide(
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Enable Mindful Shield",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HoldUp works completely on your device. Zero internet tracking or account required.",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Accessibility Row
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Interceptor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Detects app launch to show pause",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (isAccessibilityGranted) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Usage Access Row
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usage Stats",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Shows today's screen time stats",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (isUsageAccessGranted) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    OutlinedButton(
                        onClick = onOpenUsageSettings,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable")
                    }
                }
            }
        }
    }
}
