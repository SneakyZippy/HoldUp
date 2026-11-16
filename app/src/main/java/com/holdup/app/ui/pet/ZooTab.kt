package com.holdup.app.ui.pet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.DailyStats
import com.holdup.app.data.model.PetInstance
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetSpecies
import com.holdup.app.data.model.PetTreat
import com.holdup.app.data.model.ZooState

@Composable
fun ZooTab(
    zooState: ZooState,
    todayStats: DailyStats,
    onFeedPet: (PetTreat) -> Unit,
    onAdoptMysteryEgg: () -> Unit,
    onAdoptSpecificEgg: (PetSpecies) -> Unit,
    onSelectActivePet: (String) -> Unit,
    onRenamePet: (String, String) -> Unit
) {
    var selectedPetForDetail by remember { mutableStateOf<PetInstance?>(null) }
    var petToRename by remember { mutableStateOf<PetInstance?>(null) }
    var showSpecificEggPicker by remember { mutableStateOf(false) }

    val activePet = zooState.activePet
    val activeMood = activePet?.resolveMood(todayStats, todayStats.dateString) ?: PetMood.CONTENT

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ZooHeader(spendablePoints = todayStats.spendablePoints)
        }

        if (activePet != null) {
            item {
                ActivePetSanctuaryCard(
                    pet = activePet,
                    mood = activeMood,
                    onEditName = { petToRename = activePet }
                )
            }
        }

        if (zooState.canAdoptNewEgg) {
            item {
                EggAdoptionBanner(
                    isStarter = !zooState.hasClaimedStarterEgg && zooState.ownedPets.isEmpty(),
                    spendablePoints = todayStats.spendablePoints,
                    graduatedPetName = activePet?.takeIf { it.isGraduated }?.customName,
                    onAdoptMystery = onAdoptMysteryEgg,
                    onOpenSpecificPicker = { showSpecificEggPicker = true }
                )
            }
        }

        if (activePet != null) {
            item {
                FeedingTraySection(
                    mood = activeMood,
                    spendablePoints = todayStats.spendablePoints,
                    onFeedPet = onFeedPet
                )
            }
        }

        item {
            ZooCollectionSection(
                zooState = zooState,
                todayStats = todayStats,
                onPetClick = { selectedPetForDetail = it }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    selectedPetForDetail?.let { pet ->
        OwnedPetDetailDialog(
            pet = pet,
            isActive = pet.id == activePet?.id,
            mood = pet.resolveMood(todayStats, todayStats.dateString),
            onDismiss = { selectedPetForDetail = null },
            onEquip = { onSelectActivePet(pet.id) },
            onRequestRename = { petToRename = pet }
        )
    }

    petToRename?.let { pet ->
        RenamePetDialog(
            pet = pet,
            onDismiss = { petToRename = null },
            onConfirm = { newName -> onRenamePet(pet.id, newName) }
        )
    }

    if (showSpecificEggPicker) {
        SpecificEggPickerDialog(
            spendablePoints = todayStats.spendablePoints,
            ownedSpecies = zooState.ownedSpecies,
            onDismiss = { showSpecificEggPicker = false },
            onSelectSpecies = onAdoptSpecificEgg
        )
    }
}

@Composable
private fun ZooHeader(spendablePoints: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Spirit Sanctuary",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Stay mindful to feed & evolve your Zen Zoo",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$spendablePoints pts",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
