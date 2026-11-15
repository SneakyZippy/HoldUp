package com.holdup.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.ui.components.zenGlassBorder
import java.util.Calendar

private data class MindfulTip(
    val title: String,
    val body: String
)

private val DAILY_MINDFUL_TIPS = listOf(
    MindfulTip(
        title = "Friction Creates Freedom",
        body = "Opening an app isn't a failure. HoldUp gives your conscious mind a moment to decide if this is what you truly desire right now."
    ),
    MindfulTip(
        title = "The 5-Second Reset",
        body = "A single slow breath interrupts automatic muscle memory and shifts you from reflex back into choice."
    ),
    MindfulTip(
        title = "Attention Is Your Currency",
        body = "Every time you walk away from a mindless scroll, you reclaim minutes for something that genuinely nourishes you."
    ),
    MindfulTip(
        title = "Notice the Trigger",
        body = "Most compulsive opens happen during brief boredom or stress. Pause and ask: what am I actually looking for right now?"
    ),
    MindfulTip(
        title = "Intentional Sessions Win",
        body = "Using an app with a clear 5-minute timer feels completely different from drifting through an endless feed."
    )
)

@Composable
fun DailyMindfulTipCard(
    modifier: Modifier = Modifier
) {
    val tip = remember {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        DAILY_MINDFUL_TIPS[dayOfYear % DAILY_MINDFUL_TIPS.size]
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = zenGlassBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = tip.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}
