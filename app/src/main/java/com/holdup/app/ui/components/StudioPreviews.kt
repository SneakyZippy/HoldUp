package com.holdup.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoCall
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.holdup.app.data.model.AlternativeActivity
import com.holdup.app.ui.theme.*
import java.io.File

@Composable
fun BreathingPreviewBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1E17),
        border = BorderStroke(1.dp, ZenSage.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Concentric glowing rings
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(ZenSage.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(ZenSage.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(ZenSage, ZenSageDim))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = Color(0xFF090A0D),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Inhale / Exhale cues
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• Inhale",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ZenSage.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Exhale •",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ZenSage.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun ReflectionPreviewBanner(
    sampleQuote: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF181528),
        border = BorderStroke(1.dp, ZenLavender.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = ZenLavender.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "“",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZenLavender,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"$sampleQuote\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "MINDFUL THOUGHT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = ZenLavender,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

@Composable
fun PhotoPreviewBanner(
    photoUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF221616),
        border = BorderStroke(1.dp, ZenCoral.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        if (!photoUri.isNullOrBlank()) {
            val photoModel = if (photoUri.startsWith("/")) File(photoUri) else photoUri
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = photoModel,
                    contentDescription = "Loved one",
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = ZenCoral,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Photo Selected",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        )
                    }
                    Text(
                        text = "Displayed when opening shielded apps",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ZenCoral.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "CHANGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ZenCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ZenCoral.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ZenCoral.copy(alpha = 0.35f)),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = ZenCoral,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tap to choose a photo",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = "Family, partner, pet, or cherished memory",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPreviewBanner(
    videoUri: String?,
    onPreview: () -> Unit,
    onSelectVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSet = !videoUri.isNullOrBlank()
    val videoAccent = Color(0xFF38BDF8)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF101B26),
        border = BorderStroke(1.dp, videoAccent.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (isSet) onPreview() else onSelectVideo()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = videoAccent.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, videoAccent.copy(alpha = 0.35f)),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSet) Icons.Default.PlayCircle else Icons.Default.VideoCall,
                        contentDescription = null,
                        tint = videoAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSet) "Video clip configured" else "Choose a video clip",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = if (isSet) "Tap to preview playback" else "Short 5-10s clip from a friend",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSet) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = videoAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = videoAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SwapsPreviewBanner(
    activeSwaps: List<AlternativeActivity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF10221C),
        border = BorderStroke(1.dp, ZenTeal.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displaySwaps = activeSwaps.take(3).ifEmpty {
                com.holdup.app.data.model.defaultAlternativeActivities.take(3)
            }
            displaySwaps.forEach { swap ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, ZenTeal.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = swap.iconEmoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = swap.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
