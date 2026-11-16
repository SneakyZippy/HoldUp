package com.holdup.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.RelapseState
import com.holdup.app.data.model.SessionPenaltyCalculator
import com.holdup.app.ui.theme.ZenAmber
import kotlin.math.round

@Composable
internal fun ChooseDurationStep(
    isSoftNudge: Boolean,
    selectedMinutes: Int,
    sliderValue: Float,
    graceMinutesRemaining: Int,
    relapseState: RelapseState,
    onDurationChange: (Int, Float, Boolean) -> Unit,
    onEnterSession: () -> Unit,
    onBack: () -> Unit
) {
    val cost = SessionPenaltyCalculator.calculateCost(selectedMinutes, graceMinutesRemaining, relapseState)

    Text(
        text = "Set Intended Time",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            letterSpacing = (-0.2).sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = when {
            cost.isEscalated ->
                "Relapse: -${cost.totalPenaltyPoints} pts (${cost.baseRelapsePenaltyPoints} + ${cost.durationPenaltyPoints} for ${selectedMinutes}m) • Unused time refunded if closed early"
            cost.usesGrace && !cost.exceedsGrace ->
                "Uses ${cost.graceMinutesUsed}m of ${graceMinutesRemaining}m Daily Grace • 0 pts • Unused time refunded if closed early"
            cost.usesGrace && cost.exceedsGrace ->
                "Uses ${cost.graceMinutesUsed}m Grace + ${cost.overBudgetMinutes}m over (-${cost.totalPenaltyPoints} pts) • Unused time refunded if closed early"
            else ->
                "No Grace left • -${cost.totalPenaltyPoints} pts & resets Clean Focus • Unused time refunded if closed early"
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            color = when {
                cost.isEscalated -> Color(0xFFFFB74D)
                !cost.exceedsGrace -> MaterialTheme.colorScheme.primary
                else -> ZenAmber
            }
        ),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$selectedMinutes",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (selectedMinutes == 1) "minute session" else "minutes session",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Slider(
        value = sliderValue,
        onValueChange = { newValue ->
            val rounded = round(newValue).toInt().coerceIn(1, 60)
            onDurationChange(rounded, newValue, rounded != selectedMinutes)
        },
        valueRange = 1f..60f,
        steps = 58,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier.fillMaxWidth()
    )

    DurationPresetRow(selectedMinutes = selectedMinutes, onSelectPreset = { preset ->
        onDurationChange(preset, preset.toFloat(), true)
    })

    Spacer(modifier = Modifier.height(18.dp))

    Button(
        onClick = onEnterSession,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (cost.isEscalated) Color(0xFFD84315) else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
    ) {
        Text(
            text = when {
                cost.isEscalated -> "Enter for $selectedMinutes min (-${cost.totalPenaltyPoints} pts)"
                cost.usesGrace && !cost.exceedsGrace && isSoftNudge -> "Use Grace ($selectedMinutes min more)"
                cost.usesGrace && !cost.exceedsGrace -> "Use Grace ($selectedMinutes min)"
                cost.usesGrace && cost.exceedsGrace ->
                    "Use ${cost.graceMinutesUsed}m Grace + ${cost.overBudgetMinutes}m (-${cost.totalPenaltyPoints} pts)"
                isSoftNudge -> "Continue for $selectedMinutes min (-${cost.totalPenaltyPoints} pts)"
                else -> "Enter for $selectedMinutes min (-${cost.totalPenaltyPoints} pts)"
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(onClick = onBack) {
        Text(
            text = "← Changed my mind (Walk away)",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun DurationPresetRow(
    selectedMinutes: Int,
    onSelectPreset: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(2, 5, 10, 15, 30).forEach { preset ->
            val isSelected = selectedMinutes == preset
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectPreset(preset) }
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "${preset}m",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
