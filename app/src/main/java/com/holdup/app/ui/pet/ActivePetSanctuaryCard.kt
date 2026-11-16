package com.holdup.app.ui.pet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.PetInstance
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetStage
import com.holdup.app.ui.components.zenGlassBorder
import com.holdup.app.ui.theme.ZenCoral

@Composable
fun ActivePetSanctuaryCard(
    pet: PetInstance,
    mood: PetMood,
    onEditName: () -> Unit
) {
    val accent = Color(pet.species.primaryColorHex)
    val moodColor = when (mood) {
        PetMood.RADIANT -> MaterialTheme.colorScheme.primary
        PetMood.CONTENT -> MaterialTheme.colorScheme.secondary
        PetMood.SAD -> ZenCoral
    }
    val animatedProgress by animateFloatAsState(
        targetValue = pet.stageProgressFraction,
        animationSpec = tween(700),
        label = "PetXpBar"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = zenGlassBorder(isHighlighted = true, accentColor = accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SanctuaryBadgesRow(pet = pet, mood = mood, accent = accent, moodColor = moodColor)
            Spacer(modifier = Modifier.height(8.dp))
            ZenSpiritCanvas(
                pet = pet,
                mood = mood,
                modifier = Modifier.size(168.dp),
                onPat = {}
            )
            PetIdentityHeader(pet = pet, onEditName = onEditName)
            Spacer(modifier = Modifier.height(14.dp))
            PetXpProgressBar(
                pet = pet,
                animatedProgress = animatedProgress,
                accent = accent,
                mood = mood,
                moodColor = moodColor
            )
        }
    }
}

@Composable
private fun SanctuaryBadgesRow(
    pet: PetInstance,
    mood: PetMood,
    accent: Color,
    moodColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = 0.14f)
        ) {
            Text(
                text = "Stage ${pet.stage.level}/4 • ${pet.stage.label}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = moodColor.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, moodColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = mood.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = moodColor,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PetIdentityHeader(
    pet: PetInstance,
    onEditName: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = pet.customName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onEditName, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename pet",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    Text(
        text = "${pet.species.displayName} • ${pet.species.spiritTitle}",
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PetXpProgressBar(
    pet: PetInstance,
    animatedProgress: Float,
    accent: Color,
    mood: PetMood,
    moodColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(accent, Color(pet.species.secondaryColorHex))))
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    val nextLabel = when (pet.stage) {
        PetStage.EGG -> "Hatchling"
        PetStage.BABY -> "Juvenile"
        PetStage.TEEN -> "Awakened"
        PetStage.ADULT -> null
    }
    Text(
        text = if (nextLabel != null && pet.xpRequiredForNextStage != null) {
            "${pet.xpIntoCurrentStage} / ${pet.xpRequiredForNextStage} XP to $nextLabel (${pet.xp} total XP)"
        } else {
            "Awakened Spirit • ${pet.xp} total XP"
        },
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = mood.subtitle,
        style = MaterialTheme.typography.labelSmall.copy(color = moodColor, fontSize = 11.sp),
        textAlign = TextAlign.Center
    )
}
