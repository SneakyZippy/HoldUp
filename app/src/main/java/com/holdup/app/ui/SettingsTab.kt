package com.holdup.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.ui.components.PinSetupDialog
import com.holdup.app.ui.components.PinVerifyDialog
import com.holdup.app.ui.theme.*

@Composable
fun SettingsTab(
    isChildSafetyEnabled: Boolean,
    hasParentPin: Boolean,
    onSetChildSafetyEnabled: (Boolean) -> Unit,
    onSetParentPin: (String) -> Unit,
    onVerifyParentPin: suspend (String) -> Boolean,
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerifyDialog by remember { mutableStateOf(false) }
    var pinVerifyAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinVerifyTitle by remember { mutableStateOf("Enter Parent PIN") }
    var pinVerifySubtitle by remember { mutableStateOf("Verify your PIN to proceed") }
    var isChangingPin by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Settings & Protection",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Strict protection, parent PIN, and system permissions",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        // 1. Child Safety & Hard Lockout Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Hard Lockout",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = if (isChildSafetyEnabled) "Active • Instant close enabled" else "Disabled",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isChildSafetyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = isChildSafetyEnabled,
                            onCheckedChange = { willEnable ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (willEnable) {
                                    if (!hasParentPin) {
                                        isChangingPin = false
                                        showPinSetupDialog = true
                                    } else {
                                        pinVerifyTitle = "Activate Hard Lockout"
                                        pinVerifySubtitle = "Enter your PIN to enable Hard Lockout"
                                        pinVerifyAction = { onSetChildSafetyEnabled(true) }
                                        showPinVerifyDialog = true
                                    }
                                } else {
                                    pinVerifyTitle = "Disable Hard Lockout"
                                    pinVerifySubtitle = "Enter your PIN to disable Hard Lockout"
                                    pinVerifyAction = { onSetChildSafetyEnabled(false) }
                                    showPinVerifyDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Strict mode for absolute self-control or parental protection. Monitored apps close immediately upon tap with no option to enter. HoldUp requires your PIN to open or modify settings.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    if (isChildSafetyEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Active Protection Features:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Instant Close: Blocked apps close immediately upon tap\n• App Lock: HoldUp requires your PIN every time it opens",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pinVerifyTitle = "Change PIN"
                                pinVerifySubtitle = "Enter current PIN before setting a new one"
                                pinVerifyAction = {
                                    isChangingPin = true
                                    showPinSetupDialog = true
                                }
                                showPinVerifyDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Change PIN")
                        }
                    }
                }
            }
        }

        // 2. System Permissions Status Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "System Permissions",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Required to detect monitored apps and display mindful pauses",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Accessibility Item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accessibility Service",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (isAccessibilityGranted) "Active • Intercepts target app launches" else "Missing • Pauses cannot be triggered",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isAccessibilityGranted) ZenSage else Color(0xFFF97316)
                                )
                            )
                        }
                        if (!isAccessibilityGranted) {
                            Button(
                                onClick = onOpenAccessibilitySettings,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ZenSage,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Usage Access Item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usage Stats Access",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (isUsageAccessGranted) "Active • Measures time saved and app opens" else "Missing • Screen time stats disabled",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isUsageAccessGranted) ZenSage else Color(0xFFF97316)
                                )
                            )
                        }
                        if (!isUsageAccessGranted) {
                            Button(
                                onClick = onOpenUsageSettings,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ZenSage,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. App Tour & Onboarding
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "App Tour & Onboarding",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Review how HoldUp works or walk through the permission setup again.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onReplayOnboarding()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Onboard Again")
                    }
                }
            }
        }

        // 4. About HoldUp
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "HoldUp",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A mindful friction tool designed to break unconscious phone habits and return your attention to what truly matters.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onPinCreated = { newPin ->
                onSetParentPin(newPin)
                if (!isChangingPin) {
                    onSetChildSafetyEnabled(true)
                }
                showPinSetupDialog = false
            }
        )
    }

    if (showPinVerifyDialog) {
        PinVerifyDialog(
            title = pinVerifyTitle,
            subtitle = pinVerifySubtitle,
            onDismiss = { showPinVerifyDialog = false },
            onVerifyPin = onVerifyParentPin,
            onSuccess = {
                showPinVerifyDialog = false
                pinVerifyAction?.invoke()
            }
        )
    }
}
