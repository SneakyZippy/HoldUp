package com.holdup.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.HoldUpApp
import com.holdup.app.data.model.*
import com.holdup.app.service.HoldUpAccessibilityService
import com.holdup.app.service.SessionMonitorService
import com.holdup.app.ui.components.*
import com.holdup.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class InterventionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_IS_SOFT_NUDGE = "extra_is_soft_nudge"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val isSoftNudge = intent.getBooleanExtra(EXTRA_IS_SOFT_NUDGE, false)

        setContent {
            HoldUpTheme {
                InterventionScreen(
                    targetPackageName = targetPackage,
                    isSoftNudge = isSoftNudge,
                    onWalkAway = { isAlternative ->
                        val scope = (application as HoldUpApp).preferencesManager
                        // Log mindful win with points
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            scope.recordWalkAway(15, isAlternativeActivity = isAlternative)
                        }
                        // Navigate to home screen
                        val navigated = HoldUpAccessibilityService.performGoHome()
                        if (!navigated) {
                            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_HOME)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                        }
                        finishAndRemoveTask()
                    },
                    onOpenSession = { minutes ->
                        val scope = (application as HoldUpApp).preferencesManager
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            scope.recordProceedSession(targetPackage, minutes)
                        }
                        SessionMonitorService.startSession(this@InterventionActivity, targetPackage, minutes)

                        // Directly switch to the target app the user wanted to open
                        if (targetPackage.isNotBlank()) {
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(
                                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                        android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                    )
                                    startActivity(launchIntent)
                                }
                            } catch (_: Exception) {}
                        }
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }
}

@Composable
fun InterventionScreen(
    targetPackageName: String,
    isSoftNudge: Boolean,
    onWalkAway: (Boolean) -> Unit,
    onOpenSession: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val prefs = HoldUpApp.instance.preferencesManager

    var activeInterventionType by remember { mutableStateOf(InterventionType.BREATHING) }
    var breathingSeconds by remember { mutableIntStateOf(8) }
    var countdownSeconds by remember { mutableIntStateOf(10) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var photoCaption by remember { mutableStateOf("Remember what truly matters to you.") }
    var videoUri by remember { mutableStateOf<String?>(null) }
    var alternativeActivities by remember { mutableStateOf<List<AlternativeActivity>>(defaultAlternativeActivities) }
    var selectedAlternative by remember { mutableStateOf<AlternativeActivity?>(null) }
    var maxSessionMinutes by remember { mutableIntStateOf(15) }

    var isInterventionCompleted by remember { mutableStateOf(isSoftNudge) }
    var currentQuote by remember { mutableStateOf("") }
    var availableQuotes by remember { mutableStateOf<List<String>>(defaultReflections) }

    // Load app-specific or global configuration
    LaunchedEffect(targetPackageName) {
        val global = prefs.globalSettings.first()
        val appRules = prefs.perAppRules.first()
        val activities = prefs.alternativeActivities.first()
        alternativeActivities = activities

        val customQ = prefs.customReflections.first()
        val dismissedQ = prefs.dismissedReflections.first()
        val pool = (defaultReflections + customQ).filter { it !in dismissedQ }
        availableQuotes = if (pool.isNotEmpty()) pool else defaultReflections
        if (currentQuote.isEmpty() && availableQuotes.isNotEmpty()) {
            currentQuote = prefs.getNextReflectionQuote(availableQuotes)
        }

        val rule = appRules[targetPackageName]
        val activePhotoUri = rule?.customPhotoUri ?: global.lovedOnePhotoUri
        val activeVideoUri = rule?.customVideoUri ?: global.friendVideoUri

        photoUri = activePhotoUri
        photoCaption = rule?.customPhotoCaption?.ifBlank { global.lovedOneCaption } ?: global.lovedOneCaption
        videoUri = activeVideoUri
        breathingSeconds = rule?.breathingSeconds ?: global.defaultBreathingSeconds
        countdownSeconds = rule?.countdownSeconds ?: global.defaultCountdownSeconds
        maxSessionMinutes = rule?.maxSessionMinutes ?: global.defaultSessionMinutes

        if (rule != null && rule.ruleMode == RuleMode.SPECIFIC) {
            activeInterventionType = rule.specificType
        } else {
            val mode = rule?.ruleMode ?: global.defaultRuleMode
            val allPossibleTypes = listOf(
                InterventionType.BREATHING,
                InterventionType.REFLECTION,
                InterventionType.ALTERNATIVES,
                InterventionType.PHOTO,
                InterventionType.VIDEO
            )
            // Filter eligible interventions: photo & video are automatically included when media is configured
            val eligiblePool = allPossibleTypes.filter { type ->
                when (type) {
                    InterventionType.PHOTO -> !activePhotoUri.isNullOrBlank()
                    InterventionType.VIDEO -> !activeVideoUri.isNullOrBlank()
                    else -> global.enabledInterventions.contains(type)
                }
            }.ifEmpty {
                listOf(InterventionType.BREATHING, InterventionType.REFLECTION, InterventionType.ALTERNATIVES)
            }

            activeInterventionType = prefs.getNextInterventionType(mode, eligiblePool)
        }

        if (activeInterventionType == InterventionType.REFLECTION ||
            activeInterventionType == InterventionType.PHOTO ||
            activeInterventionType == InterventionType.ALTERNATIVES) {
            isInterventionCompleted = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Reality Check or Soft Nudge banner
            if (isSoftNudge) {
                SoftNudgeHeader(targetPackageName = targetPackageName)
            } else {
                RealityCheckHeader(packageName = targetPackageName)
            }

            // Body: Active Intervention Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isSoftNudge) {
                    SoftNudgeBody()
                } else {
                    when (activeInterventionType) {
                        InterventionType.BREATHING -> {
                            BreathingOrb(
                                totalSeconds = breathingSeconds,
                                onCompleted = { isInterventionCompleted = true }
                            )
                        }
                        InterventionType.REFLECTION -> {
                            ReflectionCard(
                                quote = currentQuote.ifEmpty { "Pause and take a deep breath." },
                                onNextQuote = {
                                    if (availableQuotes.isNotEmpty()) {
                                        coroutineScope.launch {
                                            currentQuote = prefs.getNextReflectionQuote(availableQuotes)
                                        }
                                    }
                                },
                                onDismissQuote = { dismissedText ->
                                    coroutineScope.launch {
                                        prefs.dismissReflection(dismissedText)
                                        val remaining = availableQuotes.filter { it != dismissedText }
                                        availableQuotes = if (remaining.isNotEmpty()) remaining else defaultReflections
                                        currentQuote = prefs.getNextReflectionQuote(availableQuotes)
                                    }
                                }
                            )
                        }
                        InterventionType.PHOTO -> {
                            LovedOneCard(
                                photoUriString = photoUri,
                                caption = photoCaption,
                                onCompleted = { isInterventionCompleted = true }
                            )
                        }
                        InterventionType.VIDEO -> {
                            VideoPlayerView(
                                videoUriString = videoUri,
                                onCompleted = { isInterventionCompleted = true }
                            )
                        }
                        InterventionType.ALTERNATIVES -> {
                            AlternativesList(
                                activities = alternativeActivities,
                                selectedActivity = selectedAlternative,
                                onSelectActivity = {
                                    selectedAlternative = if (selectedAlternative?.id == it.id) null else it
                                    isInterventionCompleted = true
                                }
                            )
                        }
                    }
                }
            }

            // Footer: Decision Controls
            AnimatedVisibility(
                visible = isInterventionCompleted,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DecisionFooter(
                    isSoftNudge = isSoftNudge,
                    maxSessionMinutes = maxSessionMinutes,
                    selectedAlternative = selectedAlternative,
                    onWalkAway = { onWalkAway(selectedAlternative != null) },
                    onOpenSession = onOpenSession
                )
            }
        }
    }
}

@Composable
fun SoftNudgeHeader(targetPackageName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = "TIME'S UP",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.5.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Session Complete",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun SoftNudgeBody() {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⏳",
            fontSize = 54.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your planned session has ended.",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Take this opportunity to close the app and return to the real world.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DecisionFooter(
    isSoftNudge: Boolean,
    maxSessionMinutes: Int = 15,
    selectedAlternative: AlternativeActivity?,
    onWalkAway: () -> Unit,
    onOpenSession: (Int) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary Choice: Walk Away
        Button(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onWalkAway()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (selectedAlternative != null) {
                    "${selectedAlternative.title} (+20 pts)"
                } else {
                    "Walk Away (+10 pts)"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Secondary Choice: Timed Session Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSoftNudge) "Grant extra:" else "Or open for:",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isSoftNudge) {
                    SessionChip(label = "+2 min") { onOpenSession(2) }
                    SessionChip(label = "+5 min") { onOpenSession(5) }
                } else {
                    SessionChip(label = "5 min") { onOpenSession(5) }
                    if (maxSessionMinutes >= 10) {
                        SessionChip(label = "10 min") { onOpenSession(10) }
                    }
                    if (maxSessionMinutes >= 15) {
                        SessionChip(label = "15 min") { onOpenSession(15) }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionChip(
    label: String,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}
