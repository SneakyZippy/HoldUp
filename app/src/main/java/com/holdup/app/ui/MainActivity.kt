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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import coil3.compose.AsyncImage
import com.holdup.app.HoldUpApp
import com.holdup.app.data.model.*
import com.holdup.app.service.HoldUpAccessibilityService
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import com.holdup.app.ui.components.*
import com.holdup.app.ui.onboarding.OnboardingScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import com.holdup.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val perAppRules by prefs.perAppRules.collectAsStateWithLifecycle(initialValue = emptyMap())
    val hasCompletedOnboarding by prefs.hasCompletedOnboarding.collectAsStateWithLifecycle(initialValue = false)
    val customReflections by prefs.customReflections.collectAsStateWithLifecycle(initialValue = emptyList())
    val dismissedReflections by prefs.dismissedReflections.collectAsStateWithLifecycle(initialValue = emptySet())
    val isChildSafetyEnabled by prefs.isChildSafetyEnabled.collectAsStateWithLifecycle(initialValue = false)
    val hasParentPin by prefs.hasParentPin.collectAsStateWithLifecycle(initialValue = false)
    var isAppUnlockedByParent by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                isAppUnlockedByParent = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isChildSafetyEnabled && !isAppUnlockedByParent) {
        ParentPinLockScreen(
            onVerifyPin = { pin ->
                prefs.verifyParentPin(pin)
            },
            onUnlocked = {
                isAppUnlockedByParent = true
            },
            onExit = {
                (context as? ComponentActivity)?.finishAffinity()
            }
        )
        return
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var showOnboardingManually by remember { mutableStateOf(false) }
    var selectedAppForRule by remember { mutableStateOf<InstalledAppItem?>(null) }
    var showQuotesBottomSheet by remember { mutableStateOf(false) }
    var showSwapsBottomSheet by remember { mutableStateOf(false) }

    var isAccessibilityGranted by remember { mutableStateOf(false) }
    var isUsageAccessGranted by remember { mutableStateOf(false) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

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

    // First-launch or manual Onboarding Tour
    if (!hasCompletedOnboarding || showOnboardingManually) {
        OnboardingScreen(
            monitoredPackages = monitoredPackages,
            onToggleApp = { pkg, isMonitored ->
                coroutineScope.launch { prefs.toggleAppMonitored(pkg, isMonitored) }
            },
            isAccessibilityGranted = isAccessibilityGranted,
            isUsageAccessGranted = isUsageAccessGranted,
            onOpenAccessibilitySettings = {
                showAccessibilityDisclosure = true
            },
            onOpenUsageSettings = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
            },
            onCompleteOnboarding = {
                coroutineScope.launch { prefs.setOnboardingCompleted(true) }
                showOnboardingManually = false
            }
        )
        if (showAccessibilityDisclosure) {
            AccessibilityDisclosureDialog(
                onDismiss = { showAccessibilityDisclosure = false },
                onAgree = {
                    showAccessibilityDisclosure = false
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = ZenBackground,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text("Apps") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text("Studio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    isChildSafetyEnabled = isChildSafetyEnabled,
                    onToggleMonitoring = { enabled ->
                        if (isChildSafetyEnabled && !enabled) {
                            android.widget.Toast.makeText(context, "Disable Hard Lockout in Settings to pause monitoring", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            coroutineScope.launch { prefs.setMonitoringEnabled(enabled) }
                        }
                    },
                    todayStats = todayStats,
                    monitoredPackages = monitoredPackages,
                    isAccessibilityGranted = isAccessibilityGranted,
                    isUsageAccessGranted = isUsageAccessGranted,
                    onOpenAccessibilitySettings = {
                        showAccessibilityDisclosure = true
                    },
                    onOpenUsageSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    onOpenTour = { showOnboardingManually = true }
                )
                1 -> AppsTab(
                    monitoredPackages = monitoredPackages,
                    perAppRules = perAppRules,
                    onToggleApp = { pkg, isMonitored ->
                        coroutineScope.launch { prefs.toggleAppMonitored(pkg, isMonitored) }
                    },
                    onConfigureApp = { appItem ->
                        selectedAppForRule = appItem
                    }
                )
                2 -> InterventionsTab(
                    globalSettings = globalSettings,
                    alternatives = alternatives,
                    customReflections = customReflections,
                    dismissedReflections = dismissedReflections,
                    onSaveSettings = { updated ->
                        coroutineScope.launch { prefs.saveGlobalSettings(updated) }
                    },
                    onAddCustomReflection = { text ->
                        coroutineScope.launch { prefs.addCustomReflection(text) }
                    },
                    onResetDismissedReflections = {
                        coroutineScope.launch { prefs.resetDismissedReflections() }
                    },
                    onOpenQuotesManager = { showQuotesBottomSheet = true },
                    onOpenSwapsManager = { showSwapsBottomSheet = true }
                )
                3 -> SettingsTab(
                    isChildSafetyEnabled = isChildSafetyEnabled,
                    hasParentPin = hasParentPin,
                    onSetChildSafetyEnabled = { enabled ->
                        coroutineScope.launch { prefs.setChildSafetyEnabled(enabled) }
                    },
                    onSetParentPin = { pin ->
                        coroutineScope.launch { prefs.setParentPin(pin) }
                    },
                    onVerifyParentPin = { pin ->
                        prefs.verifyParentPin(pin)
                    },
                    isAccessibilityGranted = isAccessibilityGranted,
                    isUsageAccessGranted = isUsageAccessGranted,
                    onOpenAccessibilitySettings = {
                        showAccessibilityDisclosure = true
                    },
                    onOpenUsageSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    onReplayOnboarding = { showOnboardingManually = true }
                )
            }
        }
    }

    selectedAppForRule?.let { appItem ->
        AppRuleBottomSheet(
            packageName = appItem.packageName,
            appName = appItem.appName,
            currentRule = perAppRules[appItem.packageName],
            onDismiss = { selectedAppForRule = null },
            onSaveRule = { rule ->
                coroutineScope.launch {
                    prefs.saveAppRule(rule)
                }
            }
        )
    }

    if (showQuotesBottomSheet) {
        QuotesManagementBottomSheet(
            customReflections = customReflections,
            dismissedReflections = dismissedReflections,
            onDismissRequest = { showQuotesBottomSheet = false },
            onAddQuote = { text -> coroutineScope.launch { prefs.addCustomReflection(text) } },
            onRemoveCustomQuote = { text -> coroutineScope.launch { prefs.removeCustomReflection(text) } },
            onDownvoteQuote = { text -> coroutineScope.launch { prefs.dismissReflection(text) } },
            onRestoreQuote = { text -> coroutineScope.launch { prefs.restoreReflection(text) } },
            onResetAllHidden = { coroutineScope.launch { prefs.resetDismissedReflections() } }
        )
    }

    if (showSwapsBottomSheet) {
        HealthySwapsBottomSheet(
            activities = alternatives,
            onDismissRequest = { showSwapsBottomSheet = false },
            onToggleActivity = { id -> coroutineScope.launch { prefs.toggleAlternativeActivity(id) } },
            onAddActivity = { title, emoji -> coroutineScope.launch { prefs.addAlternativeActivity(title, emoji) } },
            onUpdateActivity = { id, title, emoji -> coroutineScope.launch { prefs.updateAlternativeActivity(id, title, emoji) } },
            onDeleteActivity = { id -> coroutineScope.launch { prefs.deleteAlternativeActivity(id) } },
            onSetAllEnabled = { enabled -> coroutineScope.launch { prefs.setAllAlternativeActivitiesEnabled(enabled) } }
        )
    }

    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onDismiss = { showAccessibilityDisclosure = false },
            onAgree = {
                showAccessibilityDisclosure = false
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun AccessibilityDisclosureDialog(
    onDismiss: () -> Unit,
    onAgree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Accessibility Service Disclosure",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HoldUp uses Android's AccessibilityService API strictly to provide mindful friction and help break doomscrolling habits.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "How this API is used:",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "• App Detection: Detects when you launch a monitored app (e.g. social media) to present your mindful pause before scrolling.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        )
                        Text(
                            text = "• Walk Away Navigation: Automatically performs a Home navigation when you choose to walk away and save screen time.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Privacy Guarantee:",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "• ZERO personal data collection, transmission, or storage.\n• NO reading or logging of keystrokes, passwords, chats, or screen contents.\n• HoldUp operates 100% offline with zero network calls.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agree & Continue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not Now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
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
    onOpenUsageSettings: () -> Unit,
    onOpenTour: () -> Unit = {},
    isChildSafetyEnabled: Boolean = false
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = com.holdup.app.R.drawable.ic_holdup_logo),
                        contentDescription = "HoldUp Turtle Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HoldUp",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChildSafetyEnabled || isMonitoringEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                            )
                            Text(
                                text = if (isChildSafetyEnabled) "Hard Lockout Active" else if (isMonitoringEnabled) "Mindful Shield Active" else "Shield Paused",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isChildSafetyEnabled || isMonitoringEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onOpenTour,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tour",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Switch(
                    checked = isMonitoringEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleMonitoring(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }

        // Setup Alert if permissions missing
        if (!isAccessibilityGranted || !isUsageAccessGranted) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
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
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To pause doomscrolling when apps open, HoldUp needs accessibility and usage access permissions enabled.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
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
                                    Text("Enable Interceptor", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (!isUsageAccessGranted) {
                                OutlinedButton(
                                    onClick = onOpenUsageSettings,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                ) {
                                    Text("Usage Access", color = MaterialTheme.colorScheme.onErrorContainer)
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
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
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
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timelapse,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "~${todayStats.estimatedMinutesSaved}m saved",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val trackColor = Color.White.copy(alpha = 0.06f)

                    Box(
                        modifier = Modifier.size(175.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 12.dp.toPx()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = (size.minDimension - strokeWidth) / 2f

                            // Soft ambient glow behind percentage
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gaugeColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = radius * 0.85f
                                ),
                                radius = radius * 0.85f,
                                center = center
                            )

                            // Background circular track
                            drawCircle(
                                color = trackColor,
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )

                            // Animated progress arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        gaugeColor.copy(alpha = 0.7f),
                                        gaugeColor,
                                        secondaryColor
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${todayStats.successRatePercent}%",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Mindful Win Rate",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Triple Stat Pill Row
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.03f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
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
                                modifier = Modifier.height(26.dp),
                                color = Color.White.copy(alpha = 0.08f)
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
                                modifier = Modifier.height(26.dp),
                                color = Color.White.copy(alpha = 0.08f)
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

        // Mindful Points & Level Progress Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = todayStats.userLevel,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Tier ${todayStats.currentLevelNumber} • ${todayStats.lifetimePoints} total pts",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "+${todayStats.pointsEarnedToday} pts today",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val prevTierTarget = when (todayStats.currentLevelNumber) {
                        1 -> 0
                        2 -> 100
                        3 -> 300
                        4 -> 600
                        5 -> 1000
                        else -> 0
                    }
                    val targetDelta = (todayStats.nextLevelTarget - prevTierTarget).coerceAtLeast(1)
                    val currentProgress = ((todayStats.lifetimePoints - prevTierTarget).toFloat() / targetDelta.toFloat())
                        .coerceIn(0f, 1f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Next tier: ${todayStats.nextLevelTarget} pts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            val remaining = (todayStats.nextLevelTarget - todayStats.lifetimePoints).coerceAtLeast(0)
                            Text(
                                text = if (remaining > 0) "$remaining pts to go" else "Max tier reached!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.06f)
                        )
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(packageName = pkg, size = 28.dp)
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
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌱",
                        fontSize = 28.sp,
                        modifier = Modifier.padding(end = 14.dp)
                    )
                    Column {
                        Text(
                            text = "Friction Creates Freedom",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Opening an app isn't a failure. HoldUp gives your conscious mind a moment to decide if this is what you truly desire right now.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
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
    perAppRules: Map<String, AppRuleConfig>,
    onToggleApp: (String, Boolean) -> Unit,
    onConfigureApp: (InstalledAppItem) -> Unit
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
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Text(
            text = "Tap any app to customize its active hours & pause style",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.packageName }) { appItem ->
                    val isChecked = monitoredPackages.contains(appItem.packageName)
                    val rule = perAppRules[appItem.packageName]
                    val scheduleLabel = rule?.schedulePreset?.displayName ?: "24/7"
                    val styleLabel = when (rule?.ruleMode) {
                        RuleMode.SPECIFIC -> rule.specificType.displayName
                        RuleMode.SHUFFLE -> "Shuffle"
                        RuleMode.ROTATE -> "Rotate"
                        RuleMode.RANDOM -> "Random"
                        RuleMode.SEQUENCE -> "Multi-Step"
                        null -> "Rotate"
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChecked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onConfigureApp(appItem)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                packageName = appItem.packageName,
                                size = 42.dp,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appItem.appName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isChecked) "$scheduleLabel • $styleLabel" else "Tap to configure rules",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Switch(
                                checked = isChecked,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleApp(appItem.packageName, it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
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
    customReflections: List<String> = emptyList(),
    dismissedReflections: Set<String> = emptySet(),
    onSaveSettings: (GlobalInterventionSettings) -> Unit,
    onAddCustomReflection: (String) -> Unit = {},
    onResetDismissedReflections: () -> Unit = {},
    onOpenQuotesManager: () -> Unit = {},
    onOpenSwapsManager: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var activeDetailType by remember { mutableStateOf<InterventionType?>(null) }
    var showAddQuoteDialog by remember { mutableStateOf(false) }
    var newQuoteText by remember { mutableStateOf("") }
    var showVideoPreview by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val localPath = saveMediaToInternalStorage(context, uri, "loved_one_photo", "jpg")
                if (localPath != null) {
                    onSaveSettings(globalSettings.copy(lovedOnePhotoUri = localPath))
                }
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val localPath = saveMediaToInternalStorage(context, uri, "friend_video", "mp4")
                if (localPath != null) {
                    onSaveSettings(globalSettings.copy(friendVideoUri = localPath))
                }
            }
        }
    }

    LaunchedEffect(globalSettings.friendVideoUri, globalSettings.lovedOnePhotoUri) {
        withContext(Dispatchers.IO) {
            var updated = globalSettings
            var modified = false
            val currentVideo = globalSettings.friendVideoUri
            if (!currentVideo.isNullOrBlank() && currentVideo.startsWith("content://")) {
                val local = saveMediaToInternalStorage(context, Uri.parse(currentVideo), "friend_video", "mp4")
                if (local != null) {
                    updated = updated.copy(friendVideoUri = local)
                    modified = true
                }
            }
            val currentPhoto = globalSettings.lovedOnePhotoUri
            if (!currentPhoto.isNullOrBlank() && currentPhoto.startsWith("content://")) {
                val local = saveMediaToInternalStorage(context, Uri.parse(currentPhoto), "loved_one_photo", "jpg")
                if (local != null) {
                    updated = updated.copy(lovedOnePhotoUri = local)
                    modified = true
                }
            }
            if (modified) {
                onSaveSettings(updated)
            }
        }
    }

    if (showAddQuoteDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddQuoteDialog = false
                newQuoteText = ""
            },
            title = { Text("Add Mindful Thought") },
            text = {
                Column {
                    Text(
                        text = "Write a grounding question or reminder to see when opening apps:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newQuoteText,
                        onValueChange = { newQuoteText = it },
                        placeholder = { Text("e.g. Is this what I need right now?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newQuoteText.isNotBlank()) {
                            onAddCustomReflection(newQuoteText.trim())
                        }
                        showAddQuoteDialog = false
                        newQuoteText = ""
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddQuoteDialog = false
                    newQuoteText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showVideoPreview && globalSettings.friendVideoUri != null) {
        AlertDialog(
            onDismissRequest = { showVideoPreview = false },
            title = { Text("Friend's Video Preview") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VideoPlayerView(videoUriString = globalSettings.friendVideoUri)
                }
            },
            confirmButton = {
                Button(onClick = { showVideoPreview = false }) {
                    Text("Done")
                }
            }
        )
    }

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
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Tap any card to test, preview, and customize",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        // 1. Rotation Strategy Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Rotation Strategy",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "How active pauses cycle when opening target apps",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            Triple(RuleMode.ROTATE, "🔄 Rotate", "In turn"),
                            Triple(RuleMode.SHUFFLE, "🎲 Deck", "All first"),
                            Triple(RuleMode.RANDOM, "✨ Random", "Pure chance")
                        )

                        modes.forEach { (mode, label, sub) ->
                            val isSelected = globalSettings.defaultRuleMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSaveSettings(globalSettings.copy(defaultRuleMode = mode))
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Active Pause Styles
        item {
            val enabledPool = globalSettings.enabledInterventions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE PAUSE STYLES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "${enabledPool.size}/5 active",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // 2-Column Grid Row 1: Breathing + Reflection
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Breathing
                val isBreathingEnabled = globalSettings.enabledInterventions.contains(InterventionType.BREATHING)
                StudioGridCard(
                    title = "Breathing",
                    categoryBadge = "MINDFUL BREATH",
                    badgeColor = ZenSage,
                    icon = Icons.Default.Air,
                    statusText = "${globalSettings.defaultBreathingSeconds}s pause",
                    isEnabled = isBreathingEnabled,
                    onToggle = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val updated = globalSettings.enabledInterventions.toMutableList()
                        if (checked) {
                            if (!updated.contains(InterventionType.BREATHING)) updated.add(InterventionType.BREATHING)
                        } else {
                            if (updated.size > 1) updated.remove(InterventionType.BREATHING)
                        }
                        onSaveSettings(globalSettings.copy(enabledInterventions = updated))
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeDetailType = InterventionType.BREATHING
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(ZenSage.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(ZenSage.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(ZenSage, ZenSageDim))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = null,
                                        tint = Color(0xFF090A0D),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Mindful Quotes
                val isQuotesEnabled = globalSettings.enabledInterventions.contains(InterventionType.REFLECTION)
                val quotesTotal = defaultReflections.size + customReflections.size
                StudioGridCard(
                    title = "Mindful Quotes",
                    categoryBadge = "REALITY CHECK",
                    badgeColor = ZenLavender,
                    icon = Icons.Default.FormatQuote,
                    statusText = "$quotesTotal quotes",
                    isEnabled = isQuotesEnabled,
                    onToggle = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val updated = globalSettings.enabledInterventions.toMutableList()
                        if (checked) {
                            if (!updated.contains(InterventionType.REFLECTION)) updated.add(InterventionType.REFLECTION)
                        } else {
                            if (updated.size > 1) updated.remove(InterventionType.REFLECTION)
                        }
                        onSaveSettings(globalSettings.copy(enabledInterventions = updated))
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeDetailType = InterventionType.REFLECTION
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "“",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ZenLavender.copy(alpha = 0.8f),
                            modifier = Modifier.offset(y = (-3).dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Is this what I need right now?",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            ),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2-Column Grid Row 2: Photo + Video
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 3: Photo
                val isPhotoEnabled = globalSettings.enabledInterventions.contains(InterventionType.PHOTO)
                val hasPhoto = !globalSettings.lovedOnePhotoUri.isNullOrBlank()
                StudioGridCard(
                    title = "Loved One",
                    categoryBadge = "PERSONAL TETHER",
                    badgeColor = ZenCoral,
                    icon = Icons.Default.Favorite,
                    statusText = if (hasPhoto) "Photo active" else "Tap to choose",
                    isEnabled = isPhotoEnabled,
                    onToggle = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val updated = globalSettings.enabledInterventions.toMutableList()
                        if (checked) {
                            if (!updated.contains(InterventionType.PHOTO)) updated.add(InterventionType.PHOTO)
                        } else {
                            if (updated.size > 1) updated.remove(InterventionType.PHOTO)
                        }
                        onSaveSettings(globalSettings.copy(enabledInterventions = updated))
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeDetailType = InterventionType.PHOTO
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (hasPhoto) {
                        val photoModel = if (globalSettings.lovedOnePhotoUri!!.startsWith("/")) File(globalSettings.lovedOnePhotoUri!!) else globalSettings.lovedOnePhotoUri
                        AsyncImage(
                            model = photoModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = ZenCoral.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Photo",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ZenCoral,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Card 4: Video
                val isVideoEnabled = globalSettings.enabledInterventions.contains(InterventionType.VIDEO)
                val hasVideo = !globalSettings.friendVideoUri.isNullOrBlank()
                val videoColor = Color(0xFF38BDF8)
                StudioGridCard(
                    title = "Friend's Video",
                    categoryBadge = "VIDEO CUE",
                    badgeColor = videoColor,
                    icon = Icons.Default.PlayCircle,
                    statusText = if (hasVideo) "Video ready" else "Tap to choose",
                    isEnabled = isVideoEnabled,
                    onToggle = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val updated = globalSettings.enabledInterventions.toMutableList()
                        if (checked) {
                            if (!updated.contains(InterventionType.VIDEO)) updated.add(InterventionType.VIDEO)
                        } else {
                            if (updated.size > 1) updated.remove(InterventionType.VIDEO)
                        }
                        onSaveSettings(globalSettings.copy(enabledInterventions = updated))
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeDetailType = InterventionType.VIDEO
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    if (hasVideo) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = videoColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = videoColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ready",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = videoColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ Video",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = videoColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Card 5: Healthy Swaps (Wide Hero Card)
        item {
            val isSwapsEnabled = globalSettings.enabledInterventions.contains(InterventionType.ALTERNATIVES)
            val activeSwaps = alternatives.filter { it.isEnabled }
            StudioWideCard(
                title = "Healthy Swaps",
                categoryBadge = "RESTORATIVE HABITS",
                badgeColor = ZenTeal,
                icon = Icons.Default.SwapHoriz,
                statusText = "${activeSwaps.size} active alternatives",
                isEnabled = isSwapsEnabled,
                onToggle = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val updated = globalSettings.enabledInterventions.toMutableList()
                    if (checked) {
                        if (!updated.contains(InterventionType.ALTERNATIVES)) updated.add(InterventionType.ALTERNATIVES)
                    } else {
                        if (updated.size > 1) updated.remove(InterventionType.ALTERNATIVES)
                    }
                    onSaveSettings(globalSettings.copy(enabledInterventions = updated))
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    activeDetailType = InterventionType.ALTERNATIVES
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displaySwaps = activeSwaps.take(3).ifEmpty {
                        defaultAlternativeActivities.take(3)
                    }
                    displaySwaps.forEach { swap ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, ZenTeal.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = swap.iconEmoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = swap.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Detail Bottom Sheet when an intervention card is tapped
    activeDetailType?.let { type ->
        InterventionDetailBottomSheet(
            type = type,
            globalSettings = globalSettings,
            alternatives = alternatives,
            customReflections = customReflections,
            dismissedReflections = dismissedReflections,
            onSaveSettings = onSaveSettings,
            onOpenQuotesManager = onOpenQuotesManager,
            onOpenSwapsManager = onOpenSwapsManager,
            onAddQuoteClick = { showAddQuoteDialog = true },
            onPickPhotoClick = { photoPickerLauncher.launch("image/*") },
            onPickVideoClick = { videoPickerLauncher.launch("video/*") },
            onPreviewVideoClick = { showVideoPreview = true },
            onResetDismissedReflections = onResetDismissedReflections,
            onDismissRequest = { activeDetailType = null }
        )
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

private fun saveMediaToInternalStorage(context: Context, sourceUri: Uri, prefix: String, extension: String): String? {
    return try {
        val destFile = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.$extension")
        context.filesDir.listFiles { _, name -> name.startsWith(prefix) }?.forEach { it.delete() }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        android.util.Log.e("HoldUpMedia", "Failed to save media to internal storage", e)
        null
    }
}
