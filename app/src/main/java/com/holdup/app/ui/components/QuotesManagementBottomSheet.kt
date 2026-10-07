package com.holdup.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.defaultReflections

private enum class QuoteFilter {
    ALL, ACTIVE, HIDDEN, CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesManagementBottomSheet(
    customReflections: List<String>,
    dismissedReflections: Set<String>,
    onDismissRequest: () -> Unit,
    onAddQuote: (String) -> Unit,
    onRemoveCustomQuote: (String) -> Unit,
    onDownvoteQuote: (String) -> Unit,
    onRestoreQuote: (String) -> Unit,
    onResetAllHidden: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(QuoteFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newQuoteText by remember { mutableStateOf("") }

    // Combined all quotes with custom items at the top
    val allQuotes = remember(customReflections) {
        customReflections + defaultReflections
    }

    val filteredQuotes = remember(allQuotes, dismissedReflections, searchQuery, selectedFilter) {
        allQuotes.filter { quote ->
            val isCustom = quote in customReflections
            val isHidden = quote in dismissedReflections
            val matchesFilter = when (selectedFilter) {
                QuoteFilter.ALL -> true
                QuoteFilter.ACTIVE -> !isHidden
                QuoteFilter.HIDDEN -> isHidden
                QuoteFilter.CUSTOM -> isCustom
            }
            val matchesSearch = searchQuery.isBlank() || quote.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newQuoteText = ""
            },
            title = { Text("Add Mindful Thought") },
            text = {
                Column {
                    Text(
                        text = "Write a question or reminder to see when opening apps:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newQuoteText,
                        onValueChange = { newQuoteText = it },
                        placeholder = { Text("e.g. Is this intentional or habit?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newQuoteText.isNotBlank()) {
                            onAddQuote(newQuoteText.trim())
                        }
                        showAddDialog = false
                        newQuoteText = ""
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newQuoteText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mindful Quotes Library",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${allQuotes.size - dismissedReflections.size} active • ${dismissedReflections.size} hidden",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search quotes...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuoteFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        QuoteFilter.ALL -> allQuotes.size
                        QuoteFilter.ACTIVE -> allQuotes.size - dismissedReflections.size
                        QuoteFilter.HIDDEN -> dismissedReflections.size
                        QuoteFilter.CUSTOM -> customReflections.size
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = filter
                        },
                        label = {
                            Text("${filter.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)")
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (dismissedReflections.isNotEmpty() && selectedFilter == QuoteFilter.HIDDEN) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResetAllHidden()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore all (${dismissedReflections.size})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quotes List
            if (filteredQuotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No thoughts match this filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredQuotes, key = { it }) { quote ->
                        val isCustom = quote in customReflections
                        val isHidden = quote in dismissedReflections

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isHidden) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isHidden) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isHidden) 0.6f else 1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isCustom) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            ) {
                                                Text(
                                                    text = "Custom",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (isHidden) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = "Hidden",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "“$quote”",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 20.sp,
                                            fontWeight = if (isHidden) FontWeight.Normal else FontWeight.Medium
                                        ),
                                        color = if (isHidden) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Downvote / Restore toggle
                                    if (isHidden) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onRestoreQuote(quote)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Unhide quote",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onDownvoteQuote(quote)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ThumbDown,
                                                contentDescription = "Hide / Downvote quote",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Permanently delete custom quote
                                    if (isCustom) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onRemoveCustomQuote(quote)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete custom quote",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
