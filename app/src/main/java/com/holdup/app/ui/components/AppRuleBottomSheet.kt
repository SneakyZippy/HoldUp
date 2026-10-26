package com.holdup.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.*

private data class RuleOptionItem(
    val mode: RuleMode,
    val type: InterventionType?,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRuleBottomSheet(
    packageName: String,
    appName: String,
    currentRule: AppRuleConfig?,
    onDismiss: () -> Unit,
    onSaveRule: (AppRuleConfig) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val initialRule = currentRule ?: AppRuleConfig(packageName = packageName)
    var selectedPreset by remember { mutableStateOf(initialRule.schedulePreset) }
    var selectedMode by remember { mutableStateOf(initialRule.ruleMode) }
    var selectedType by remember { mutableStateOf(initialRule.specificType) }
    var pauseSeconds by remember { mutableIntStateOf(initialRule.breathingSeconds) }
    var maxSessionMinutes by remember { mutableIntStateOf(initialRule.maxSessionMinutes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                AppIcon(
                    packageName = packageName,
                    size = 46.dp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "Customize mindful rules",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Active Hours Presets
            Text(
                text = "Active Schedule",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "When should HoldUp introduce mindful pauses?",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SchedulePreset.values().forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedPreset = preset
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Intervention Style
            Text(
                text = "Intervention Style",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Choose what plays when $appName opens. Tap to select, or Test to preview:",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(10.dp))

            val ruleOptions = listOf(
                RuleOptionItem(
                    RuleMode.ROTATE,
                    null,
                    "🔄 Rotate in Turn",
                    "Cycles evenly through your rotation pool"
                ),
                RuleOptionItem(
                    RuleMode.SHUFFLE,
                    null,
                    "🎲 Shuffle Deck",
                    "Balanced shuffle before repeating"
                ),
                RuleOptionItem(
                    RuleMode.SPECIFIC,
                    InterventionType.BREATHING,
                    "🌬️ Rhythmic Breathing",
                    "Visual breath orb & haptic cadence"
                ),
                RuleOptionItem(
                    RuleMode.SPECIFIC,
                    InterventionType.REFLECTION,
                    "💭 Mindful Reflection",
                    "Reality checks & intention prompts"
                ),
                RuleOptionItem(
                    RuleMode.SPECIFIC,
                    InterventionType.ALTERNATIVES,
                    "⚡ Healthy Swaps",
                    "Rewarding activities instead of doomscrolling"
                ),
                RuleOptionItem(
                    RuleMode.SPECIFIC,
                    InterventionType.PHOTO,
                    "🖼️ Loved One's Photo",
                    "Heartfelt emotional grounding reminder"
                ),
                RuleOptionItem(
                    RuleMode.SPECIFIC,
                    InterventionType.VIDEO,
                    "🎬 Friend's Video",
                    "Short personal video message"
                )
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ruleOptions.forEach { item ->
                    val isSelected = if (item.mode == RuleMode.SPECIFIC) {
                        selectedMode == RuleMode.SPECIFIC && selectedType == item.type
                    } else {
                        selectedMode == item.mode
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.White.copy(alpha = 0.025f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = item.mode
                                if (item.type != null) {
                                    selectedType = item.type
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            // Immediate Test button
                            val testType = item.type ?: InterventionType.BREATHING
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.holdup.app.ui.InterventionActivity.startTest(
                                        context = context,
                                        type = testType,
                                        targetPackage = initialRule.packageName
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Test",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Max Allowed Session Pass
            Text(
                text = "Session Pass Options",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Maximum timer duration to offer if you choose to enter",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 15).forEach { mins ->
                    val isSelected = maxSessionMinutes == mins
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                maxSessionMinutes = mins
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Up to ${mins}m",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val updated = initialRule.copy(
                        schedulePreset = selectedPreset,
                        ruleMode = selectedMode,
                        specificType = selectedType,
                        breathingSeconds = pauseSeconds,
                        countdownSeconds = pauseSeconds,
                        maxSessionMinutes = maxSessionMinutes
                    )
                    onSaveRule(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = "Save Rule",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

}
