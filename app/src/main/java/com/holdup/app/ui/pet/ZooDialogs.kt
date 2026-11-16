package com.holdup.app.ui.pet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.PetInstance
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetSpecies
import com.holdup.app.data.preferences.PetPreferencesRepository
import com.holdup.app.ui.components.zenGlassBorder

@Composable
fun RenamePetDialog(
    pet: PetInstance,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draftName by remember(pet.id) { mutableStateOf(pet.customName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Your Spirit") },
        text = {
            Column {
                Text(
                    text = "Give your ${pet.species.displayName} a custom name:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { if (it.length <= 24) draftName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (draftName.isNotBlank()) onConfirm(draftName.trim())
                    onDismiss()
                },
                enabled = draftName.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SpecificEggPickerDialog(
    spendablePoints: Int,
    ownedSpecies: Set<PetSpecies>,
    onDismiss: () -> Unit,
    onSelectSpecies: (PetSpecies) -> Unit
) {
    val canAfford = spendablePoints >= PetPreferencesRepository.SPECIFIC_EGG_COST
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Specific Egg (${PetPreferencesRepository.SPECIFIC_EGG_COST} pts)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select the exact Zen Spirit you want to nurture next:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PetSpecies.entries.forEach { species ->
                    val alreadyOwned = species in ownedSpecies
                    val primary = Color(species.primaryColorHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = canAfford) {
                                onSelectSpecies(species)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = primary.copy(alpha = 0.16f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                ZenSpiritCanvas(
                                    pet = PetInstance(id = "preview", species = species, customName = species.defaultPetName, xp = 2100),
                                    mood = PetMood.CONTENT,
                                    isCompact = true
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = species.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (alreadyOwned) "${species.biome} • Collected" else "${species.biome} • New!",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (alreadyOwned) MaterialTheme.colorScheme.onSurfaceVariant else primary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun OwnedPetDetailDialog(
    pet: PetInstance,
    isActive: Boolean,
    mood: PetMood,
    onDismiss: () -> Unit,
    onEquip: () -> Unit,
    onRequestRename: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pet.customName) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZenSpiritCanvas(
                    pet = pet,
                    mood = mood,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${pet.species.displayName} • ${pet.stage.label}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${pet.species.spiritTitle} • ${pet.xp} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = zenGlassBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (pet.isGraduated) {
                            "Awakened Spirit living peacefully in your Zen Zoo."
                        } else {
                            "Currently growing (${pet.stageProgressFraction.times(100).toInt()}% of ${pet.stage.label})."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (!isActive) {
                Button(onClick = {
                    onEquip()
                    onDismiss()
                }) { Text("Set Active") }
            } else {
                Button(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onRequestRename()
            }) { Text("Rename") }
        }
    )
}
