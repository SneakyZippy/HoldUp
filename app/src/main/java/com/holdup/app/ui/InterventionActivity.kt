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
                    onWalkAway = {
                        val scope = (application as HoldUpApp).preferencesManager
                        // Log mindful win
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            scope.recordWalkAway(15)
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
                        finish()
                    },
                    onOpenSession = { minutes ->
                        val scope = (application as HoldUpApp).preferencesManager
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            scope.recordProceedSession(targetPackage, minutes)
                        }
                        SessionMonitorService.startSession(this@InterventionActivity, targetPackage, minutes)
                        finish()
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
    onWalkAway: () -> Unit,
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

    var isInterventionCompleted by remember { mutableStateOf(isSoftNudge) }

    // Load app-specific or global configuration
    LaunchedEffect(targetPackageName) {
        val global = prefs.globalSettings.first()
        val appRules = prefs.perAppRules.first()
        val activities = prefs.alternativeActivities.first()
        alternativeActivities = activities

        val rule = appRules[targetPackageName]
        if (rule != null) {
            when (rule.ruleMode) {
                RuleMode.SPECIFIC -> {
                    activeInterventionType = rule.specificType
                }
                RuleMode.SHUFFLE, RuleMode.SEQUENCE -> {
                    val enabled = global.enabledInterventions.ifEmpty { listOf(InterventionType.BREATHING) }
                    activeInterventionType = enabled[Random.nextInt(enabled.size)]
                }
            }
            breathingSeconds = rule.breathingSeconds
            countdownSeconds = rule.countdownSeconds
            photoUri = rule.customPhotoUri ?: global.lovedOnePhotoUri
            photoCaption = rule.customPhotoCaption.ifBlank { global.lovedOneCaption }
            videoUri = rule.customVideoUri ?: global.friendVideoUri
        } else {
            val enabled = global.enabledInterventions.ifEmpty { listOf(InterventionType.BREATHING) }
            activeInterventionType = enabled[Random.nextInt(enabled.size)]
            breathingSeconds = global.defaultBreathingSeconds
            countdownSeconds = global.defaultCountdownSeconds
            photoUri = global.lovedOnePhotoUri
            photoCaption = global.lovedOneCaption
            videoUri = global.friendVideoUri
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
                        InterventionType.COUNTDOWN -> {
                            DelayCountdownBar(
                                totalSeconds = countdownSeconds,
                                onCompleted = { isInterventionCompleted = true }
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
                                    selectedAlternative = it
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
                    selectedAlternative = selectedAlternative,
                    onWalkAway = onWalkAway,
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
        Text(
            text = "TIME'S UP",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 3.sp,
                color = ZenCoral,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Session Complete",
            style = MaterialTheme.typography.headlineMedium
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
            color = ZenTextSecondary
        )
    }
}

@Composable
fun DecisionFooter(
    isSoftNudge: Boolean,
    selectedAlternative: AlternativeActivity?,
    onWalkAway: () -> Unit,
    onOpenSession: (Int) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(5) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary Choice: Walk Away
        Button(
            onClick = onWalkAway,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZenSage,
                contentColor = ZenBackground
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedAlternative != null) {
                    "Walk Away & ${selectedAlternative.title}"
                } else {
                    "Walk Away (Mindful Win)"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary Choice: Timed Session Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSoftNudge) "Grant +2 min:" else "Or open for:",
                style = MaterialTheme.typography.labelSmall.copy(color = ZenTextSecondary)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isSoftNudge) {
                    SessionChip(label = "+2 min") { onOpenSession(2) }
                    SessionChip(label = "+5 min") { onOpenSession(5) }
                } else {
                    SessionChip(label = "5 min") { onOpenSession(5) }
                    SessionChip(label = "10 min") { onOpenSession(10) }
                    SessionChip(label = "15 min") { onOpenSession(15) }
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ZenSurfaceVariant)
            .border(1.dp, ZenBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = ZenTextPrimary
            )
        )
    }
}
