package com.holdup.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun LovedOneCard(
    photoUriString: String?,
    caption: String,
    onCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(4000L)
        onCompleted()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .size(210.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUriString.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(photoUriString),
                        contentDescription = "Loved One's Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Someone who loves you",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "\"$caption\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
        }
    }
}
