package com.holdup.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.ZooState
import com.holdup.app.ui.components.zenGlassBorder
import com.holdup.app.ui.pet.ZenSpiritCanvas
import com.holdup.app.ui.theme.ZenCoral

@Composable
fun DashboardCompanionCard(
    zooState: ZooState,
    todayStats: DailyStats,
    onClick: () -> Unit
) {
    val activePet = zooState.activePet
    val mood = activePet?.resolveMood(todayStats, todayStats.dateString) ?: PetMood.CONTENT
    val accent = activePet?.let { Color(it.species.primaryColorHex) } ?: MaterialTheme.colorScheme.primary
    val moodColor = when (mood) {
        PetMood.RADIANT -> MaterialTheme.colorScheme.primary
        PetMood.CONTENT -> MaterialTheme.colorScheme.secondary
        PetMood.SAD -> ZenCoral
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = zenGlassBorder(isHighlighted = activePet != null, accentColor = accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZenSpiritCanvas(
                pet = activePet,
                mood = mood,
                isCompact = true,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activePet?.customName ?: "Spirit Sanctuary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = moodColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, moodColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (activePet != null) mood.label else "Start Egg",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = moodColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (activePet != null) {
                        "${activePet.stage.label} • ${activePet.species.displayName}"
                    } else {
                        "Tap to claim your free Spirit Egg"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(8.dp))
                val progress = activePet?.stageProgressFraction ?: 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(accent, MaterialTheme.colorScheme.secondary)))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (activePet != null && !activePet.isGraduated && activePet.xpRequiredForNextStage != null) {
                        "${activePet.xpIntoCurrentStage}/${activePet.xpRequiredForNextStage} XP • ${todayStats.spendablePoints} pts to spend"
                    } else {
                        "Zoo: ${zooState.ownedPets.size} pets • ${todayStats.spendablePoints} pts to spend"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Zoo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
