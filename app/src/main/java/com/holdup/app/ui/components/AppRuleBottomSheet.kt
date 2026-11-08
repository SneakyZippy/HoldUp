package com.holdup.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import com.holdup.app.ui.InterventionActivity

private enum class AppPauseSource {
    STUDIO_PLAYLIST,
    FIXED_PAUSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRuleBottomSheet(
    packageName: String,
    appName: String,
    currentRule: AppRuleConfig?,
    onDismiss: () -> Unit,
    onSaveRule: (AppRuleConfig) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val initialRule = currentRule ?: AppRuleConfig(packageName = packageName)

    var selectedPreset by remember { mutableStateOf(initialRule.schedulePreset) }
    var pauseSource by remember {
        mutableStateOf(
            if (initialRule.ruleMode == RuleMode.SPECIFIC) AppPauseSource.FIXED_PAUSE
            else AppPauseSource.STUDIO_PLAYLIST
        )
    }
    var selectedPlaylistMode by remember {
        mutableStateOf(if (initialRule.ruleMode == RuleMode.SPECIFIC) RuleMode.SHUFFLE else initialRule.ruleMode)
    }
    var selectedSpecificType by remember { mutableStateOf(initialRule.specificType) }
    var maxSessionMinutes by remember { mutableIntStateOf(initialRule.maxSessionMinutes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            HeaderSection(packageName = packageName, appName = appName)
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 14.dp))

            ScheduleSection(
                selectedPreset = selectedPreset,
                onSelectPreset = { selectedPreset = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            PauseModeSection(
                pauseSource = pauseSource,
                onSelectPauseSource = { pauseSource = it },
                selectedPlaylistMode = selectedPlaylistMode,
                onSelectPlaylistMode = { selectedPlaylistMode = it },
                selectedSpecificType = selectedSpecificType,
                onSelectSpecificType = { selectedSpecificType = it },
                packageName = packageName
            )

            Spacer(modifier = Modifier.height(20.dp))

            SessionPassSection(
                selectedMinutes = maxSessionMinutes,
                onSelectMinutes = { maxSessionMinutes = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val ruleMode = if (pauseSource == AppPauseSource.FIXED_PAUSE) RuleMode.SPECIFIC else selectedPlaylistMode
                    val updated = initialRule.copy(
                        schedulePreset = selectedPreset,
                        ruleMode = ruleMode,
                        specificType = selectedSpecificType,
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
                Text(text = "Save Rule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun HeaderSection(packageName: String, appName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(packageName = packageName, size = 46.dp, modifier = Modifier.clip(RoundedCornerShape(12.dp)))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = appName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
            Text(text = "Configure mindful pauses", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}

@Composable
private fun ScheduleSection(
    selectedPreset: SchedulePreset,
    onSelectPreset: (SchedulePreset) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Text(text = "Active Schedule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    Text(text = "When should HoldUp introduce mindful friction?", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    Spacer(modifier = Modifier.height(10.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SchedulePreset.entries.forEach { preset ->
            val isSelected = selectedPreset == preset
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectPreset(preset)
                    }
            ) {
                Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = preset.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface))
                    Text(text = preset.subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }
    }
}

@Composable
private fun PauseModeSection(
    pauseSource: AppPauseSource,
    onSelectPauseSource: (AppPauseSource) -> Unit,
    selectedPlaylistMode: RuleMode,
    onSelectPlaylistMode: (RuleMode) -> Unit,
    selectedSpecificType: InterventionType,
    onSelectSpecificType: (InterventionType) -> Unit,
    packageName: String
) {
    val haptic = LocalHapticFeedback.current

    Text(text = "Mindful Pause For This App", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    Text(text = "Choose between your Studio rotation playlist or locking to one fixed pause:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    Spacer(modifier = Modifier.height(10.dp))

    // Card 1: Studio Playlist
    val isPlaylist = pauseSource == AppPauseSource.STUDIO_PLAYLIST
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPlaylist) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, if (isPlaylist) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectPauseSource(AppPauseSource.STUDIO_PLAYLIST)
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = if (isPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Follow Studio Playlist (Recommended)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = if (isPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface))
                    Text(text = "Cycles through all pauses enabled in your Studio", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                }
                RadioButton(selected = isPlaylist, onClick = { onSelectPauseSource(AppPauseSource.STUDIO_PLAYLIST) }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
            }

            AnimatedVisibility(visible = isPlaylist) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(text = "Play order for this app:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(RuleMode.SHUFFLE to "🎲 Smart Shuffle", RuleMode.ROTATE to "🔁 In Sequence", RuleMode.RANDOM to "✨ Random").forEach { (mode, label) ->
                            val isModeSelected = selectedPlaylistMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isModeSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, if (isModeSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectPlaylistMode(mode)
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Normal, color = if (isModeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Card 2: Lock to Fixed Pause
    val isFixed = pauseSource == AppPauseSource.FIXED_PAUSE
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isFixed) MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, if (isFixed) MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectPauseSource(AppPauseSource.FIXED_PAUSE)
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = if (isFixed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Lock to a Single Pause", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = if (isFixed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface))
                    Text(text = "Always show the exact same exercise whenever this app opens", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp))
                }
                RadioButton(selected = isFixed, onClick = { onSelectPauseSource(AppPauseSource.FIXED_PAUSE) }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary))
            }

            AnimatedVisibility(visible = isFixed) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val specificItems = listOf(
                        InterventionType.BREATHING to ("🌬️ Breathing" to "Rhythmic breath orb with haptics"),
                        InterventionType.REFLECTION to ("💭 Reflection" to "Reality check quote & intention prompt"),
                        InterventionType.ALTERNATIVES to ("⚡ Healthy Swaps" to "Suggestions of rewarding alternatives"),
                        InterventionType.PHOTO to ("🖼️ Loved One" to "Personal photo & grounding message"),
                        InterventionType.VIDEO to ("🎬 Friend's Video" to "Short personal video reminder")
                    )

                    specificItems.forEach { (type, info) ->
                        val (title, subtitle) = info
                        val isItemSel = selectedSpecificType == type
                        SpecificPauseRow(
                            title = title,
                            subtitle = subtitle,
                            isSelected = isItemSel,
                            onSelect = { onSelectSpecificType(type) },
                            onTest = {
                                InterventionActivity.startTest(
                                    context = it,
                                    type = type,
                                    targetPackage = packageName
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecificPauseRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTest: (android.content.Context) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.02f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect()
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                } else {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTest(context)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Test", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SessionPassSection(
    selectedMinutes: Int,
    onSelectMinutes: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Text(text = "Session Pass Timer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    Text(text = "Allowed timer duration to offer if you choose to enter:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    Spacer(modifier = Modifier.height(10.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(5, 10, 15).forEach { mins ->
            val isSelected = selectedMinutes == mins
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMinutes(mins)
                    }
            ) {
                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Up to ${mins}m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }
}
