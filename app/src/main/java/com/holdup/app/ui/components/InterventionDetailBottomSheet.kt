package com.holdup.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.holdup.app.ui.InterventionActivity
import com.holdup.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterventionDetailBottomSheet(
    type: InterventionType,
    globalSettings: GlobalInterventionSettings,
    alternatives: List<AlternativeActivity>,
    customReflections: List<String>,
    dismissedReflections: Set<String>,
    onSaveSettings: (GlobalInterventionSettings) -> Unit,
    onOpenQuotesManager: () -> Unit,
    onOpenSwapsManager: () -> Unit,
    onAddQuoteClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onPickVideoClick: () -> Unit,
    onPreviewVideoClick: () -> Unit,
    onResetDismissedReflections: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isEnabled = globalSettings.enabledInterventions.contains(type)
    val accentColor = when (type) {
        InterventionType.BREATHING -> ZenSage
        InterventionType.REFLECTION -> ZenLavender
        InterventionType.PHOTO -> ZenCoral
        InterventionType.VIDEO -> Color(0xFF38BDF8)
        InterventionType.ALTERNATIVES -> ZenTeal
    }

    val (badgeText, titleText, iconVector) = when (type) {
        InterventionType.BREATHING -> Triple("MINDFUL BREATH", "Rhythmic Breathing", Icons.Default.Air)
        InterventionType.REFLECTION -> Triple("REALITY CHECK", "Mindful Reflection", Icons.Default.FormatQuote)
        InterventionType.PHOTO -> Triple("PERSONAL TETHER", "Loved One's Photo", Icons.Default.Favorite)
        InterventionType.VIDEO -> Triple("VIDEO CUE", "Friend's Video Message", Icons.Default.PlayCircle)
        InterventionType.ALTERNATIVES -> Triple("RESTORATIVE HABITS", "Healthy Swaps", Icons.Default.SwapHoriz)
    }

    var captionText by remember(globalSettings.lovedOneCaption) { mutableStateOf(globalSettings.lovedOneCaption) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    letterSpacing = 0.8.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            InterventionActivity.startTest(context, type)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.12f),
                            contentColor = accentColor
                        ),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val updated = globalSettings.enabledInterventions.toMutableList()
                            if (checked) {
                                if (!updated.contains(type)) updated.add(type)
                            } else {
                                if (updated.size > 1) updated.remove(type)
                            }
                            onSaveSettings(globalSettings.copy(enabledInterventions = updated))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Preview Hero Banner
            when (type) {
                InterventionType.BREATHING -> {
                    BreathingPreviewBanner()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pulsating orb & rhythmic haptics to interrupt impulsive scrolling and restore focus.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pause Duration: ${globalSettings.defaultBreathingSeconds} seconds",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 8, 12, 16, 20).forEach { sec ->
                            val isSelected = globalSettings.defaultBreathingSeconds == sec
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ZenSage.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ZenSage.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSaveSettings(globalSettings.copy(defaultBreathingSeconds = sec))
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ZenSage else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                InterventionType.REFLECTION -> {
                    val previewQuote = defaultReflections.firstOrNull() ?: "Is this an intentional choice, or pure reflex?"
                    ReflectionPreviewBanner(sampleQuote = previewQuote)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Curated thoughts & reality checks. Tap 👎 during a pause to hide any quote you don't like.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenQuotesManager,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quotes (${defaultReflections.size + customReflections.size})", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onAddQuoteClick,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }

                    if (dismissedReflections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onResetDismissedReflections()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Restore ${dismissedReflections.size} hidden quote${if (dismissedReflections.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                InterventionType.PHOTO -> {
                    PhotoPreviewBanner(
                        photoUri = globalSettings.lovedOnePhotoUri,
                        onClick = onPickPhotoClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A heartfelt visual reminder of someone you cherish to restore emotional grounding.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!globalSettings.lovedOnePhotoUri.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPickPhotoClick,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Photo", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSaveSettings(globalSettings.copy(lovedOnePhotoUri = null))
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Button(
                            onClick = onPickPhotoClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Photo", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = captionText,
                        onValueChange = {
                            captionText = it
                            onSaveSettings(globalSettings.copy(lovedOneCaption = it))
                        },
                        label = { Text("Personal Caption / Note") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                        )
                    )
                }

                InterventionType.VIDEO -> {
                    VideoPreviewBanner(
                        videoUri = globalSettings.friendVideoUri,
                        onPreview = onPreviewVideoClick,
                        onSelectVideo = onPickVideoClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A short video from a friend or mentor to speak truth into mindless scrolling.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPickVideoClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!globalSettings.friendVideoUri.isNullOrBlank()) "Change Video" else "Choose Video Clip",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!globalSettings.friendVideoUri.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = onPreviewVideoClick,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSaveSettings(globalSettings.copy(friendVideoUri = null))
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                InterventionType.ALTERNATIVES -> {
                    val activeSwaps = alternatives.filter { it.isEnabled }
                    SwapsPreviewBanner(
                        activeSwaps = activeSwaps,
                        onClick = onOpenSwapsManager
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instant suggestions of rewarding physical and mental activities instead of opening apps.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenSwapsManager,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Swaps (${activeSwaps.size} active)", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onOpenSwapsManager,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
