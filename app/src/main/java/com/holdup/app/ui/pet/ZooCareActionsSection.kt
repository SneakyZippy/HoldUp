package com.holdup.app.ui.pet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetTreat
import com.holdup.app.data.preferences.PetPreferencesRepository
import com.holdup.app.ui.components.zenGlassBorder
import com.holdup.app.ui.theme.ZenAmber

@Composable
fun EggAdoptionBanner(
    isStarter: Boolean,
    spendablePoints: Int,
    graduatedPetName: String?,
    onAdoptMystery: () -> Unit,
    onOpenSpecificPicker: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Egg, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = when {
                            isStarter -> "Claim Your Starter Spirit Egg"
                            graduatedPetName != null -> "$graduatedPetName Awakened! Adopt Next Egg"
                            else -> "Adopt a New Spirit Egg"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isStarter) "Free starter companion to start your Zoo" else "Mystery Eggs are 75% off compared to choosing a species",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            if (isStarter) {
                Button(onClick = onAdoptMystery, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Adopt Starter Egg (Free)", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAdoptMystery,
                        enabled = spendablePoints >= PetPreferencesRepository.MYSTERY_EGG_COST,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mystery (${PetPreferencesRepository.MYSTERY_EGG_COST} pts)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenSpecificPicker,
                        enabled = spendablePoints >= PetPreferencesRepository.SPECIFIC_EGG_COST,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Choose (${PetPreferencesRepository.SPECIFIC_EGG_COST} pts)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun FeedingTraySection(
    mood: PetMood,
    spendablePoints: Int,
    onFeedPet: (PetTreat) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NOURISH SPIRIT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = when (mood) {
                    PetMood.SAD -> "Feed once to cheer up"
                    PetMood.RADIANT -> "1.25× Radiant XP active"
                    PetMood.CONTENT -> "Stay Pure Zen for 1.25×"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (mood == PetMood.SAD) ZenAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetTreat.entries.forEach { treat ->
                TreatCard(
                    treat = treat,
                    mood = mood,
                    canAfford = spendablePoints >= treat.costPoints,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFeedPet(treat)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TreatCard(
    treat: PetTreat,
    mood: PetMood,
    canAfford: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = when (treat) {
        PetTreat.DEWDROP_BERRY -> "🫐"
        PetTreat.SUNLIT_LOTUS -> "🪷"
        PetTreat.CELESTIAL_NECTAR -> "✨"
    }
    val xpLabel = when {
        mood == PetMood.SAD && treat == PetTreat.DEWDROP_BERRY -> "Cheer Up"
        mood == PetMood.SAD && treat == PetTreat.SUNLIT_LOTUS -> "+${treat.baseXp} XP"
        treat == PetTreat.CELESTIAL_NECTAR || mood == PetMood.RADIANT -> "+${(treat.baseXp * 1.25f).toInt()} XP"
        else -> "+${treat.baseXp} XP"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (canAfford) MaterialTheme.colorScheme.surfaceContainer else Color.White.copy(alpha = 0.02f),
        border = zenGlassBorder(isHighlighted = canAfford, accentColor = MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = canAfford) { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = treat.title.substringAfter(" "),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )
            Text(
                text = xpLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${treat.costPoints} pts",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}
