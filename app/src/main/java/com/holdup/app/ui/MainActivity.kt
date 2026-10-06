package com.holdup.app.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holdup.app.HoldUpApp
import com.holdup.app.data.model.*
import com.holdup.app.service.HoldUpAccessibilityService
import com.holdup.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HoldUpTheme {
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

    var currentTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Apps, 2: Interventions

    // Permission check states
    var isAccessibilityGranted by remember { mutableStateOf(false) }
    var isUsageAccessGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        isAccessibilityGranted = HoldUpAccessibilityService.instance != null
        isUsageAccessGranted = checkUsageStatsPermission(context)
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    Scaffold(
        containerColor = ZenBackground,
        bottomBar = {
            NavigationBar(
                containerColor = ZenSurface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ZenSage,
                        selectedTextColor = ZenSage,
                        indicatorColor = ZenSageContainer,
                        unselectedIconColor = ZenTextSecondary,
                        unselectedTextColor = ZenTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text("Apps") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ZenSage,
                        selectedTextColor = ZenSage,
                        indicatorColor = ZenSageContainer,
                        unselectedIconColor = ZenTextSecondary,
                        unselectedTextColor = ZenTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text("Interventions") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ZenSage,
                        selectedTextColor = ZenSage,
                        indicatorColor = ZenSageContainer,
                        unselectedIconColor = ZenTextSecondary,
                        unselectedTextColor = ZenTextSecondary
                    )
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
                    isAccessibilityGranted = isAccessibilityGranted,
                    isUsageAccessGranted = isUsageAccessGranted,
                    onOpenAccessibilitySettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onOpenUsageSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    monitoredCount = monitoredPackages.size
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
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    monitoredCount: Int
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HoldUp",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Mindful Friction for Dopamine Loops",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Switch(
                    checked = isMonitoringEnabled,
                    onCheckedChange = onToggleMonitoring,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ZenSage,
                        checkedTrackColor = ZenSageContainer,
                        uncheckedThumbColor = ZenTextSecondary,
                        uncheckedTrackColor = ZenSurfaceVariant
                    )
                )
            }
        }

        // Permissions Alert if missing
        if (!isAccessibilityGranted || !isUsageAccessGranted) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ZenSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ZenCoral
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Setup Required",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To pause doomscrolling when apps open, HoldUp needs accessibility and usage access permissions.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!isAccessibilityGranted) {
                                Button(
                                    onClick = onOpenAccessibilitySettings,
                                    colors = ButtonDefaults.buttonColors(containerColor = ZenSage, contentColor = ZenBackground),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Enable Interceptor")
                                }
                            }
                            if (!isUsageAccessGranted) {
                                OutlinedButton(
                                    onClick = onOpenUsageSettings,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Usage Access", color = ZenTextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Stats Hero Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ZenSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TODAY'S MINDFUL WINS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            color = ZenSage,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${todayStats.successRatePercent}%",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZenSage
                                )
                            )
                            Text(
                                text = "Walk-Away Rate",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "~${todayStats.estimatedMinutesSaved}m",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 32.sp,
                                    color = ZenLavender
                                )
                            )
                            Text(
                                text = "Screen Time Reclaimed",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ZenSurfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${todayStats.walkedAwayCount}",
                                style = MaterialTheme.typography.titleLarge.copy(color = ZenSage)
                            )
                            Text(text = "Walked Away", style = MaterialTheme.typography.labelSmall)
                        }

                        VerticalDivider(
                            modifier = Modifier.height(28.dp),
                            color = ZenBorder
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${todayStats.proceededCount}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(text = "Mindful Opens", style = MaterialTheme.typography.labelSmall)
                        }

                        VerticalDivider(
                            modifier = Modifier.height(28.dp),
                            color = ZenBorder
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$monitoredCount",
                                style = MaterialTheme.typography.titleLarge.copy(color = ZenLavender)
                            )
                            Text(text = "Shielded Apps", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Philosophy / Guidance Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ZenSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
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
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Opening an app isn't a crime. HoldUp merely gives your conscious mind 5 seconds to decide if it's what you truly want.",
                            style = MaterialTheme.typography.bodyMedium
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
                .sortedWith(compareByDescending<InstalledAppItem> { monitoredPackages.contains(it.packageName) }.thenBy { it.appName })

            installedApps = apps
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Monitored Apps",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Choose which apps HoldUp will introduce mindful pauses for",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = ZenTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ZenTextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ZenSage,
                unfocusedBorderColor = ZenBorder,
                focusedContainerColor = ZenSurface,
                unfocusedContainerColor = ZenSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZenSage)
            }
        } else {
            val filtered = installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.packageName }) { appItem ->
                    val isChecked = monitoredPackages.contains(appItem.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ZenSurface)
                            .border(1.dp, if (isChecked) ZenSage.copy(alpha = 0.5f) else ZenBorder, RoundedCornerShape(14.dp))
                            .clickable { onToggleApp(appItem.packageName, !isChecked) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appItem.appName,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                            )
                            Text(
                                text = appItem.packageName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Switch(
                            checked = isChecked,
                            onCheckedChange = { onToggleApp(appItem.packageName, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ZenSage,
                                checkedTrackColor = ZenSageContainer,
                                uncheckedThumbColor = ZenTextSecondary,
                                uncheckedTrackColor = ZenSurfaceVariant
                            )
                        )
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

    // Media Pickers
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Intervention Studio",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Customize your mindful pause experience",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Breathing Duration Setting
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ZenSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🌬️ Breathing Exercise Duration",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current: ${globalSettings.defaultBreathingSeconds} seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenSage
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 8, 12, 16).forEach { sec ->
                            val isSelected = globalSettings.defaultBreathingSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ZenSageContainer else ZenSurfaceVariant)
                                    .border(1.dp, if (isSelected) ZenSage else ZenBorder, RoundedCornerShape(10.dp))
                                    .clickable { onSaveSettings(globalSettings.copy(defaultBreathingSeconds = sec)) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    color = if (isSelected) ZenSage else ZenTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Loved One's Photo
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ZenSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "❤️ Photo of a Loved One",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shows a personal reminder of someone who matters deeply to you.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = ZenLavender, contentColor = ZenBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (globalSettings.lovedOnePhotoUri != null) "Change Photo" else "Select Photo")
                        }

                        if (globalSettings.lovedOnePhotoUri != null) {
                            TextButton(onClick = { onSaveSettings(globalSettings.copy(lovedOnePhotoUri = null)) }) {
                                Text("Remove", color = ZenCoral)
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
                        label = { Text("Personal Quote / Note") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ZenSage,
                            unfocusedBorderColor = ZenBorder
                        )
                    )
                }
            }
        }

        // Friend's Video Message
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ZenSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🎬 Friend's Video Clip",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pick a short video clip from a friend to ground you when opening apps.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = ZenSage, contentColor = ZenBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (globalSettings.friendVideoUri != null) "Change Video" else "Choose Video Clip")
                        }

                        if (globalSettings.friendVideoUri != null) {
                            TextButton(onClick = { onSaveSettings(globalSettings.copy(friendVideoUri = null)) }) {
                                Text("Remove", color = ZenCoral)
                            }
                        }
                    }
                }
            }
        }

        // Calm Countdown Setting
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ZenSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZenBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "⏳ Calm Countdown Pause",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current: ${globalSettings.defaultCountdownSeconds} seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZenSage
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { sec ->
                            val isSelected = globalSettings.defaultCountdownSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ZenSageContainer else ZenSurfaceVariant)
                                    .border(1.dp, if (isSelected) ZenSage else ZenBorder, RoundedCornerShape(10.dp))
                                    .clickable { onSaveSettings(globalSettings.copy(defaultCountdownSeconds = sec)) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    color = if (isSelected) ZenSage else ZenTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
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
