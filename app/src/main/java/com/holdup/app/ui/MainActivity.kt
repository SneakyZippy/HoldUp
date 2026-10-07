package com.holdup.app.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.holdup.app.HoldUpApp
import com.holdup.app.data.model.*
import com.holdup.app.service.HoldUpAccessibilityService
import com.holdup.app.ui.components.AppIcon
import com.holdup.app.ui.theme.HoldUpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HoldUpTheme(dynamicColor = true) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = HoldUpApp.instance.preferencesManager

    val isMonitoringEnabled by prefs.isMonitoringEnabled.collectAsStateWithLifecycle(initialValue = true)
    val monitoredPackages by prefs.monitoredPackages.collectAsStateWithLifecycle(initialValue = emptySet())
    val todayStats by prefs.todayStats.collectAsStateWithLifecycle(initialValue = DailyStats())
    val globalSettings by prefs.globalSettings.collectAsStateWithLifecycle(initialValue = GlobalInterventionSettings())
    val alternatives by prefs.alternativeActivities.collectAsStateWithLifecycle(initialValue = defaultAlternativeActivities)

    var currentTab by remember { mutableIntStateOf(0) }

    var isAccessibilityGranted by remember { mutableStateOf(false) }
    var isUsageAccessGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        isAccessibilityGranted = checkAccessibilityPermission(context)
        isUsageAccessGranted = checkUsageStatsPermission(context)
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        checkPermissions()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text("Studio") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            when (currentTab) {
                0 -> DashboardTab(
                    isMonitoringEnabled = isMonitoringEnabled,
                    onToggleMonitoring = { enabled ->
                        coroutineScope.launch { prefs.setMonitoringEnabled(enabled) }
                    },
                    todayStats = todayStats,
                    monitoredPackages = monitoredPackages,
                    isAccessibilityGranted = isAccessibilityGranted,
                    isUsageAccessGranted = isUsageAccessGranted,
                    onOpenAccessibilitySettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onOpenUsageSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                1 -> AppsTab(
                    monitoredPackages = monitoredPackages,
                    onToggleApp = { pkg, isMonitored ->
                        coroutineScope.launch { prefs.toggleAppMonitored(pkg, isMonitored) }
                    }
                )
                2 -> InterventionsTab(
                    globalSettings = globalSettings,
                    alternatives = alternatives,
                    onSaveSettings = { updated ->
                        coroutineScope.launch { prefs.saveGlobalSettings(updated) }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    isMonitoringEnabled: Boolean,
    onToggleMonitoring: (Boolean) -> Unit,
    todayStats: DailyStats,
    monitoredPackages: Set<String>,
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val installedShieldedPackages = remember(monitoredPackages) {
        val pm = context.packageManager
        monitoredPackages.filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar & Master Toggle
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HoldUp",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = if (isMonitoringEnabled) "Mindful shield is active" else "Shield paused",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isMonitoringEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Switch(
                    checked = isMonitoringEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleMonitoring(it)
                    }
                )
            }
        }

        // Setup Alert if permissions missing
        if (!isAccessibilityGranted || !isUsageAccessGranted) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Setup Required",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To pause doomscrolling when apps open, HoldUp needs accessibility and usage access permissions enabled.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!isAccessibilityGranted) {
                                Button(
                                    onClick = onOpenAccessibilitySettings,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Enable Interceptor")
                                }
                            }
                            if (!isUsageAccessGranted) {
                                OutlinedButton(
                                    onClick = onOpenUsageSettings,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Usage Access")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mindful Wins Gauge Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S INTENTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timelapse,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "~${todayStats.estimatedMinutesSaved}m saved",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Circular Progress Dial
                    val targetProgress = if (todayStats.totalInterceptions > 0) {
                        (todayStats.walkedAwayCount.toFloat() / todayStats.totalInterceptions.toFloat())
                    } else {
                        1f
                    }
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(1200),
                        label = "GaugeProgress"
                    )

                    val gaugeColor = MaterialTheme.colorScheme.primary
                    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            // Background track
                            drawArc(
                                color = trackColor,
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            // Animated progress arc
                            drawArc(
                                color = gaugeColor,
                                startAngle = 135f,
                                sweepAngle = 270f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${todayStats.successRatePercent}%",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Walk-Away Rate",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Triple Stat Pill Row
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${todayStats.walkedAwayCount}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(text = "Walked Away", style = MaterialTheme.typography.labelSmall)
                            }

                            VerticalDivider(
                                modifier = Modifier.height(28.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${todayStats.proceededCount}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(text = "Mindful Opens", style = MaterialTheme.typography.labelSmall)
                            }

                            VerticalDivider(
                                modifier = Modifier.height(28.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${installedShieldedPackages.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(text = "Shielded Apps", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Active Shielded Apps Quick Carousel
        if (installedShieldedPackages.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Active Shields",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(installedShieldedPackages) { pkg ->
                            val appName = remember(pkg) {
                                try {
                                    val pm = context.packageManager
                                    val appInfo = pm.getApplicationInfo(pkg, 0)
                                    pm.getApplicationLabel(appInfo).toString()
                                } catch (_: Exception) {
                                    pkg.substringAfterLast(".")
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(packageName = pkg, size = 32.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mindfulness Principle Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌱",
                        fontSize = 32.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column {
                        Text(
                            text = "Friction Creates Freedom",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Opening an app isn't a failure. HoldUp gives your conscious mind a moment to decide if this is what you truly desire right now.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AppsTab(
    monitoredPackages: Set<String>,
    onToggleApp: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val apps = resolveInfos
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    InstalledAppItem(
                        packageName = info.activityInfo.packageName,
                        appName = info.loadLabel(pm).toString(),
                        isMonitored = monitoredPackages.contains(info.activityInfo.packageName)
                    )
                }
                .distinctBy { it.packageName }
                .sortedWith(
                    compareByDescending<InstalledAppItem> { monitoredPackages.contains(it.packageName) }
                        .thenBy { it.appName.lowercase() }
                )

            installedApps = apps
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Monitored Apps",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Toggle which apps HoldUp will introduce mindful pauses for",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val filtered = installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.packageName }) { appItem ->
                    val isChecked = monitoredPackages.contains(appItem.packageName)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isChecked) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChecked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleApp(appItem.packageName, !isChecked)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // High-res actual app icon!
                            AppIcon(
                                packageName = appItem.packageName,
                                size = 46.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appItem.appName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = appItem.packageName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Switch(
                                checked = isChecked,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleApp(appItem.packageName, it)
                                }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun InterventionsTab(
    globalSettings: GlobalInterventionSettings,
    alternatives: List<AlternativeActivity>,
    onSaveSettings: (GlobalInterventionSettings) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onSaveSettings(globalSettings.copy(lovedOnePhotoUri = uri.toString()))
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onSaveSettings(globalSettings.copy(friendVideoUri = uri.toString()))
        }
    }

    var captionText by remember(globalSettings.lovedOneCaption) { mutableStateOf(globalSettings.lovedOneCaption) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Intervention Studio",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Customize your mindful pause and media cues",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        // Breathing Duration Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌬️", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                        Text(
                            text = "Breathing Exercise",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current: ${globalSettings.defaultBreathingSeconds} seconds",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 8, 12, 16).forEach { sec ->
                            val isSelected = globalSettings.defaultBreathingSeconds == sec
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSaveSettings(globalSettings.copy(defaultBreathingSeconds = sec))
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loved One's Photo
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❤️", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                        Text(
                            text = "Photo of a Loved One",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A heartfelt visual reminder of someone you cherish.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (globalSettings.lovedOnePhotoUri != null) "Change Photo" else "Select Photo")
                        }

                        if (globalSettings.lovedOnePhotoUri != null) {
                            TextButton(onClick = { onSaveSettings(globalSettings.copy(lovedOnePhotoUri = null)) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = captionText,
                        onValueChange = {
                            captionText = it
                            onSaveSettings(globalSettings.copy(lovedOneCaption = it))
                        },
                        label = { Text("Personal Caption / Note") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // Friend's Video Message
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎬", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                        Text(
                            text = "Friend's Video Message",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A short video from a friend to ground you when opening apps.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (globalSettings.friendVideoUri != null) "Change Video" else "Choose Video Clip")
                        }

                        if (globalSettings.friendVideoUri != null) {
                            TextButton(onClick = { onSaveSettings(globalSettings.copy(friendVideoUri = null)) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Calm Countdown Setting
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏳", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                        Text(
                            text = "Calm Delay Bar",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current: ${globalSettings.defaultCountdownSeconds} seconds",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { sec ->
                            val isSelected = globalSettings.defaultCountdownSeconds == sec
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSaveSettings(globalSettings.copy(defaultCountdownSeconds = sec))
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkAccessibilityPermission(context: Context): Boolean {
    if (HoldUpAccessibilityService.instance != null) return true
    return try {
        val enabledSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        enabledSetting.contains(context.packageName, ignoreCase = true) &&
                enabledSetting.contains("HoldUpAccessibilityService", ignoreCase = true)
    } catch (_: Exception) {
        false
    }
}
