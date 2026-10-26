package com.holdup.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudioGridCard(
    title: String,
    categoryBadge: String,
    badgeColor: Color,
    icon: ImageVector,
    statusText: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualContent: @Composable BoxScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (isEnabled) badgeColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Icon + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.75f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Visual Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                visualContent()
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Info
            Text(
                text = categoryBadge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    letterSpacing = 0.6.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudioWideCard(
    title: String,
    categoryBadge: String,
    badgeColor: Color,
    icon: ImageVector,
    statusText: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualContent: @Composable BoxScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (isEnabled) badgeColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = categoryBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                visualContent()
            }
        }
    }
}
