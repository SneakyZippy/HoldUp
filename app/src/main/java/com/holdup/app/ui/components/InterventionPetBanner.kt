package com.holdup.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.DailyStats
import com.holdup.app.data.model.PetInstance
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.preferences.PetPreferencesRepository
import com.holdup.app.ui.pet.ZenSpiritCanvas
import com.holdup.app.ui.theme.ZenAmber

@Composable
fun InterventionPetBanner(
    activePet: PetInstance?,
    todayStats: DailyStats,
    graceMinutesRemaining: Int,
    isEscalated: Boolean,
    modifier: Modifier = Modifier
) {
    if (activePet == null) return

    val mood = activePet.resolveMood(todayStats, todayStats.dateString)
    val willHurtPet = graceMinutesRemaining <= 0 || isEscalated
    val canEarnWalkAwayBonus = todayStats.walkAwayPointsToday < PetPreferencesRepository.MAX_DAILY_WALK_AWAY_POINTS
    val borderColor = if (willHurtPet) ZenAmber else Color(activePet.species.primaryColorHex)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = borderColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZenSpiritCanvas(
                pet = activePet,
                mood = mood,
                isCompact = true,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${activePet.customName} • ${activePet.stage.label} (${mood.label})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                )
                Text(
                    text = when {
                        willHurtPet -> "Proceeding over budget will make ${activePet.customName} Sluggish. Walk away to protect their growth!"
                        canEarnWalkAwayBonus -> "${activePet.customName} is rooting for you! Walk away for +${PetPreferencesRepository.WALK_AWAY_REWARD_POINTS} treat pts."
                        else -> "${activePet.customName} is cheering you on! Stay within Grace to keep them Radiant."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}
