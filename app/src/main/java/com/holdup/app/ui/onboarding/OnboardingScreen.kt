package com.holdup.app.ui.onboarding

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.preferences.PreferencesManager
import com.holdup.app.ui.components.AppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TYPICAL_TARGET_PACKAGES = setOf(
    "com.instagram.android",              // Instagram
    "com.zhiliaoapp.musically",           // TikTok
    "com.ss.android.ugc.trill",           // TikTok alternative pkg
    "com.twitter.android",                // X / Twitter
    "com.google.android.youtube",         // YouTube
    "com.reddit.frontpage",               // Reddit
    "com.facebook.katana",                // Facebook
    "com.facebook.lite",                  // Facebook Lite
    "com.snapchat.android",               // Snapchat
    "com.instagram.barcelona",            // Threads
    "com.pinterest",                      // Pinterest
    "com.linkedin.android",               // LinkedIn
    "tv.twitch.android.app",              // Twitch
    "com.netflix.mediaclient",            // Netflix
    "bsky.app",                           // Bluesky
    "com.discord"                         // Discord
)

private data class OnboardingAppEntry(
    val packageName: String,
    val appName: String,
    val isTypical: Boolean
)

@Composable
fun OnboardingScreen(
    monitoredPackages: Set<String>,
    onToggleApp: (String, Boolean) -> Unit,
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentPage by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val isSelected = currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 32.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                    )
                }
            }

            // Body Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (currentPage) {
                    0 -> OnboardingWelcomeSlide()
                    1 -> OnboardingShieldsSlide(
                        monitoredPackages = monitoredPackages,
                        onToggleApp = onToggleApp
                    )
                    2 -> OnboardingPermissionsSlide(
                        isAccessibilityGranted = isAccessibilityGranted,
                        isUsageAccessGranted = isUsageAccessGranted,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenUsageSettings = onOpenUsageSettings
                    )
                }
            }

            // Bottom Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentPage--
                        }
                    ) {
                        Text(
                            text = "Back",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentPage < 2) {
                            currentPage++
                        } else {
                            onCompleteOnboarding()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(26.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 54.dp)
                        .shadow(8.dp, RoundedCornerShape(26.dp))
                ) {
                    Text(
                        text = if (currentPage == 2) "Start HoldUp" else "Continue",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (currentPage == 2) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcomeSlide() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .shadow(6.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.holdup.app.R.drawable.ic_holdup_logo),
                contentDescription = "HoldUp Logo",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Pause Before\nYou Scroll",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Opening social media is often pure reflex. HoldUp introduces a gentle 5-second breath before addictive apps open, breaking compulsive habit loops.",
            style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
        )
    }
}

@Composable
fun OnboardingShieldsSlide(
    monitoredPackages: Set<String>,
    onToggleApp: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var allInstalledApps by remember { mutableStateOf<List<OnboardingAppEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showOtherApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val launcherPackages = mutableSetOf<String>()
            val apps = mutableListOf<OnboardingAppEntry>()

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg != context.packageName) {
                    launcherPackages.add(pkg)
                    val label = try {
                        info.loadLabel(pm).toString()
                    } catch (_: Exception) {
                        pkg.substringAfterLast(".")
                    }
                    apps.add(
                        OnboardingAppEntry(
                            packageName = pkg,
                            appName = label,
                            isTypical = TYPICAL_TARGET_PACKAGES.contains(pkg)
                        )
                    )
                }
            }

            // Also check if any known typical doomscrolling package is installed even if omitted from launcher
            for (pkg in TYPICAL_TARGET_PACKAGES) {
                if (!launcherPackages.contains(pkg)) {
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val label = pm.getApplicationLabel(appInfo).toString()
                        apps.add(
                            OnboardingAppEntry(
                                packageName = pkg,
                                appName = label,
                                isTypical = true
                            )
                        )
                    } catch (_: Exception) {}
                }
            }

            allInstalledApps = apps
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
            isLoading = false
        }
    }

    val typicalApps = remember(allInstalledApps) {
        allInstalledApps.filter { it.isTypical }
    }
    val otherApps = remember(allInstalledApps) {
        allInstalledApps.filter { !it.isTypical }
    }

    val isSearching = searchQuery.trim().isNotEmpty()
    val searchResults = remember(allInstalledApps, searchQuery) {
        if (!isSearching) emptyList()
        else {
            val query = searchQuery.trim()
            allInstalledApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Choose What to Shield",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select apps where you want mindful pauses",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // 1. Reassuring reminder banner
                item(key = "header_reminder") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "You can always change them later in the Apps tab",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                // 2. Search bar
                item(key = "header_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search installed apps...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Search Results Mode
                if (isSearching) {
                    item(key = "search_results_header") {
                        Text(
                            text = "Search Results (${searchResults.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    if (searchResults.isEmpty()) {
                        item(key = "search_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No apps found matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    } else {
                        items(searchResults, key = { "search_${it.packageName}" }) { app ->
                            val isChecked = monitoredPackages.contains(app.packageName)
                            OnboardingAppCard(
                                packageName = app.packageName,
                                appName = app.appName,
                                isChecked = isChecked,
                                onToggle = { onToggleApp(app.packageName, it) }
                            )
                        }
                    }
                } else {
                    // 4. Default Mode: Typical apps followed by All Other Apps
                    if (typicalApps.isNotEmpty()) {
                        item(key = "typical_apps_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = "Typical Apps to Intercept",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Suggested",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        items(typicalApps, key = { "typical_${it.packageName}" }) { app ->
                            val isChecked = monitoredPackages.contains(app.packageName)
                            OnboardingAppCard(
                                packageName = app.packageName,
                                appName = app.appName,
                                isChecked = isChecked,
                                onToggle = { onToggleApp(app.packageName, it) }
                            )
                        }
                    }

                    // Section for All Other Apps
                    val otherAppsList = if (typicalApps.isEmpty()) allInstalledApps else otherApps
                    if (otherAppsList.isNotEmpty()) {
                        val monitoredOther = otherAppsList.filter { monitoredPackages.contains(it.packageName) }
                        val unmonitoredOther = otherAppsList.filter { !monitoredPackages.contains(it.packageName) }
                        val isExpanded = showOtherApps || typicalApps.isEmpty()

                        item(key = "other_apps_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = if (typicalApps.isEmpty()) "Installed Apps" else "All Other Apps",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "${otherAppsList.size} apps",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // If collapsed, display any non-typical apps that are already monitored so user selections are always visible
                        if (!isExpanded && monitoredOther.isNotEmpty()) {
                            items(monitoredOther, key = { "monitored_other_${it.packageName}" }) { app ->
                                OnboardingAppCard(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    isChecked = true,
                                    onToggle = { onToggleApp(app.packageName, it) }
                                )
                            }
                        }

                        if (!isExpanded) {
                            item(key = "expand_other_button") {
                                val countLabel = if (monitoredOther.isEmpty()) {
                                    "Show all ${otherAppsList.size} other apps"
                                } else {
                                    "Show remaining ${unmonitoredOther.size} apps"
                                }
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showOtherApps = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = countLabel,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Expanded mode: list all other apps
                            items(otherAppsList, key = { "all_other_${it.packageName}" }) { app ->
                                val isChecked = monitoredPackages.contains(app.packageName)
                                OnboardingAppCard(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    isChecked = isChecked,
                                    onToggle = { onToggleApp(app.packageName, it) }
                                )
                            }

                            if (typicalApps.isNotEmpty()) {
                                item(key = "collapse_other_button") {
                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showOtherApps = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Hide other apps",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OnboardingAppCard(
    packageName: String,
    appName: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isChecked) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle(!isChecked)
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = packageName, size = 42.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
fun OnboardingPermissionsSlide(
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enable Your Shield",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HoldUp runs completely offline on your device. No analytics, tracking, or network accounts.",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Accessibility Row
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isAccessibilityGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Interceptor",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isAccessibilityGranted) "Active & protecting" else "Detects launch to show pause",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isAccessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (isAccessibilityGranted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Enable",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Usage Access Row
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUsageAccessGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Screen Time Stats",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isUsageAccessGranted) "Active & calculating" else "Displays daily screen time",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isUsageAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (isUsageAccessGranted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = onOpenUsageSettings,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Enable",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
