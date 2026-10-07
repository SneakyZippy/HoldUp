package com.holdup.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.AlternativeActivity

@Composable
fun AlternativesList(
    activities: List<AlternativeActivity>,
    selectedActivity: AlternativeActivity?,
    onSelectActivity: (AlternativeActivity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "What would serve you better right now?",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(activities) { activity ->
                val isSelected = activity.id == selectedActivity?.id
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectActivity(activity) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activity.iconEmoji,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 14.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            )
                            Text(
                                text = activity.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
