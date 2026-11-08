package com.holdup.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
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
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class InterventionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_IS_SOFT_NUDGE = "extra_is_soft_nudge"
        const val EXTRA_IS_TEST_MODE = "extra_is_test_mode"
        const val EXTRA_TEST_INTERVENTION_TYPE = "extra_test_intervention_type"

        @Volatile
        var isInterventionActive: Boolean = false
            private set

        @Volatile
        var activeTargetPackage: String? = null
            private set

        fun startTest(context: Context, type: InterventionType, targetPackage: String = "com.google.android.youtube") {
            val intent = Intent(context, InterventionActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, targetPackage)
                putExtra(EXTRA_IS_TEST_MODE, true)
                putExtra(EXTRA_TEST_INTERVENTION_TYPE, type.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTestMode = intent.getBooleanExtra(EXTRA_IS_TEST_MODE, false)
        val rawPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val targetPackage = if (rawPackage.isBlank() && isTestMode) "com.google.android.youtube" else rawPackage
        val isSoftNudge = intent.getBooleanExtra(EXTRA_IS_SOFT_NUDGE, false)
        val testTypeName = intent.getStringExtra(EXTRA_TEST_INTERVENTION_TYPE)
        val testType = testTypeName?.let {
            try { InterventionType.valueOf(it) } catch (_: Exception) { null }
        }

        if (targetPackage.isNotBlank()) {
            isInterventionActive = true
            activeTargetPackage = targetPackage
        }

        setContent {
            val accentPreset by (application as HoldUpApp).preferencesManager.accentPreset
                .collectAsStateWithLifecycle(initialValue = AccentPreset.DEFAULT)
            HoldUpTheme(accentPreset = accentPreset) {
                InterventionScreen(
                    targetPackageName = targetPackage,
                    isSoftNudge = isSoftNudge,
                    isTestMode = isTestMode,
                    testType = testType,
                    onWalkAway = { isAlternative ->
                        isInterventionActive = false
                        activeTargetPackage = null
                        if (targetPackage.isNotBlank()) {
                            HoldUpAccessibilityService.suppressInterception(targetPackage, 1500L)
                        }
                        if (isTestMode) {
                            val pts = if (isAlternative) 20 else 10
                            Toast.makeText(
                                this@InterventionActivity,
                                "Test complete: Walk Away (+$pts mindful pts)",
                                Toast.LENGTH_SHORT
                            ).show()
                            finishAndRemoveTask()
                            return@InterventionScreen
                        }
                        val app = application as HoldUpApp
                        // Log mindful win with points using applicationScope to prevent cancellation on task removal
                        app.applicationScope.launch {
                            app.preferencesManager.recordWalkAway(15, isAlternativeActivity = isAlternative)
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
                    onOpenSession = { minutes, isPenalized ->
                        isInterventionActive = false
                        activeTargetPackage = null
                        if (targetPackage.isNotBlank()) {
                            HoldUpAccessibilityService.suppressInterception(targetPackage, 2500L)
                        }
                        if (isTestMode) {
                            val msg = "Test complete: ${minutes}m session selected" + if (isPenalized) " (-5 pts penalty)" else ""
                            Toast.makeText(this@InterventionActivity, msg, Toast.LENGTH_SHORT).show()
                            finishAndRemoveTask()
                            return@InterventionScreen
                        }
                        val app = application as HoldUpApp
                        val appContext = applicationContext
                        app.applicationScope.launch {
                            if (isPenalized) {
                                app.preferencesManager.recordProceedPenalty(5)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(appContext, "⚠️ 5 mindful points deducted for relapse", Toast.LENGTH_SHORT).show()
                                }
                            }
                            app.preferencesManager.recordProceedSession(targetPackage, minutes)
                            app.preferencesManager.saveLastChosenDuration(targetPackage, minutes)
                        }
                        SessionMonitorService.startSession(this@InterventionActivity, targetPackage, minutes)

                        // Directly switch to the target app the user wanted to open without resetting its task
                        if (targetPackage.isNotBlank()) {
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                                if (launchIntent != null) {
                                    launchIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newTarget = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (!newTarget.isNullOrBlank()) {
            isInterventionActive = true
            activeTargetPackage = newTarget
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val current = intent?.getStringExtra(EXTRA_PACKAGE_NAME)
        if (activeTargetPackage == current) {
            isInterventionActive = false
            activeTargetPackage = null
        }
    }
}

@Composable
fun InterventionScreen(
    targetPackageName: String,
    isSoftNudge: Boolean,
    isTestMode: Boolean = false,
    testType: InterventionType? = null,
    onWalkAway: (Boolean) -> Unit,
    onOpenSession: (Int, Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val prefs = HoldUpApp.instance.preferencesManager

    var activeInterventionType by remember { mutableStateOf(InterventionType.BREATHING) }
    var breathingSeconds by remember { mutableIntStateOf(8) }
    var countdownSeconds by remember { mutableIntStateOf(10) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var photoCaption by remember { mutableStateOf("Remember what truly matters to you.") }
    var videoUri by remember { mutableStateOf<String?>(null) }
    var allAlternativeActivities by remember { mutableStateOf<List<AlternativeActivity>>(defaultAlternativeActivities) }
    var displayedAlternatives by remember { mutableStateOf<List<AlternativeActivity>>(emptyList()) }
    var selectedAlternative by remember { mutableStateOf<AlternativeActivity?>(null) }
    var maxSessionMinutes by remember { mutableIntStateOf(15) }

    var relapseState by remember { mutableStateOf(RelapseState()) }
    var relapseRemainingSeconds by remember { mutableIntStateOf(0) }
    var isInterventionCompleted by remember { mutableStateOf(isSoftNudge) }
    var currentQuote by remember { mutableStateOf("") }
    var availableQuotes by remember { mutableStateOf<List<String>>(defaultReflections) }

    // Countdown effect for Relapse Friction delay
    LaunchedEffect(relapseRemainingSeconds) {
        if (relapseRemainingSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            relapseRemainingSeconds -= 1
            if (relapseRemainingSeconds == 0) {
                if (activeInterventionType != InterventionType.BREATHING &&
                    activeInterventionType != InterventionType.VIDEO) {
                    isInterventionCompleted = true
                }
            }
        }
    }

    // Load app-specific or global configuration
    LaunchedEffect(targetPackageName) {
        val global = prefs.globalSettings.first()
        val appRules = prefs.perAppRules.first()
        val activities = prefs.alternativeActivities.first()
        allAlternativeActivities = activities
        val activePool = activities.filter { it.isEnabled }.ifEmpty { activities }
        displayedAlternatives = activePool.shuffled().take(3)

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
        val rememberedDuration = prefs.getLastChosenDuration(targetPackageName)
        maxSessionMinutes = rememberedDuration ?: rule?.maxSessionMinutes ?: global.defaultSessionMinutes

        if (!isSoftNudge && !isTestMode && targetPackageName.isNotBlank()) {
            val state = prefs.checkRelapseState(targetPackageName)
            relapseState = state
            if (state.isEscalated) {
                relapseRemainingSeconds = state.delaySeconds
                isInterventionCompleted = false
            }
        }

        if (testType != null) {
            activeInterventionType = testType
        } else if (rule != null && rule.ruleMode == RuleMode.SPECIFIC) {
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
            if (!relapseState.isEscalated) {
                isInterventionCompleted = true
            }
        }
    }

    BackHandler {
        onWalkAway(false)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(isSoftNudge) {
        if (isSoftNudge) {
            triggerMindfulSessionCompleteAlert(context)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header: Live Preview Pill, Reality Check or Soft Nudge banner
                if (isTestMode) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧪 Live Preview • ${activeInterventionType.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { onWalkAway(false) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Exit Preview",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isSoftNudge) {
                    SoftNudgeHeader(targetPackageName = targetPackageName)
                } else {
                    RealityCheckHeader(packageName = targetPackageName)
                }

                if (relapseState.isEscalated) {
                    Spacer(modifier = Modifier.height(12.dp))
                    RelapseFrictionBanner(
                        state = relapseState,
                        remainingSeconds = relapseRemainingSeconds
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Body: Active Intervention Content
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSoftNudge) {
                        SoftNudgeBody()
                    } else {
                        when (activeInterventionType) {
                            InterventionType.BREATHING -> {
                                BreathingOrb(
                                    totalSeconds = maxOf(breathingSeconds, relapseRemainingSeconds),
                                    onCompleted = {
                                        if (relapseRemainingSeconds <= 0) {
                                            isInterventionCompleted = true
                                        }
                                    }
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
                                    activities = displayedAlternatives.ifEmpty { allAlternativeActivities.take(3) },
                                    selectedActivity = selectedAlternative,
                                    onSelectActivity = {
                                        selectedAlternative = if (selectedAlternative?.id == it.id) null else it
                                        isInterventionCompleted = true
                                    },
                                    onShuffle = {
                                        val activePool = allAlternativeActivities.filter { it.isEnabled }.ifEmpty { allAlternativeActivities }
                                        displayedAlternatives = activePool.shuffled().take(3)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Decision Controls
            AnimatedVisibility(
                visible = isInterventionCompleted,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DecisionFooter(
                    isSoftNudge = isSoftNudge,
                    targetPackageName = targetPackageName,
                    maxSessionMinutes = maxSessionMinutes,
                    relapseState = relapseState,
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

enum class DecisionStep {
    ASK_ENTER,
    CHOOSE_DURATION
}

@Composable
fun DecisionFooter(
    isSoftNudge: Boolean,
    targetPackageName: String,
    maxSessionMinutes: Int = 15,
    relapseState: RelapseState = RelapseState(),
    selectedAlternative: AlternativeActivity?,
    onWalkAway: () -> Unit,
    onOpenSession: (Int, Boolean) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val appName = remember(targetPackageName) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(targetPackageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            ""
        }
    }

    var currentStep by remember { mutableStateOf(DecisionStep.ASK_ENTER) }
    var selectedMinutes by remember { mutableIntStateOf(if (maxSessionMinutes in 1..60) maxSessionMinutes else 10) }
    var sliderValue by remember { mutableFloatStateOf(selectedMinutes.toFloat()) }

    BackHandler(enabled = true) {
        if (currentStep == DecisionStep.CHOOSE_DURATION) {
            currentStep = DecisionStep.ASK_ENTER
        } else {
            onWalkAway()
        }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState == DecisionStep.CHOOSE_DURATION) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "DecisionStepAnimation"
    ) { step ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                DecisionStep.ASK_ENTER -> {
                    Text(
                        text = if (isSoftNudge) "Ready to step back?" else "Still want to enter?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.2).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isSoftNudge) {
                            "Your planned session has ended. Step away for clarity."
                        } else if (selectedAlternative != null) {
                            "Selected: ${selectedAlternative.title}"
                        } else {
                            "Give yourself the gift of intentional presence."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Mindful Choice: Walk away (No)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onWalkAway()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedAlternative != null) {
                                    "${selectedAlternative.title} (+20 pts)"
                                } else {
                                    "Walk Away (+10 pts)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Choice: Yes -> advances to slider
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                currentStep = DecisionStep.CHOOSE_DURATION
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (appName.isNotBlank()) {
                                    if (relapseState.isEscalated) "Open $appName mindfully (-5 pts)" else "Open $appName mindfully"
                                } else {
                                    if (relapseState.isEscalated) "Continue mindfully (-5 pts)" else "Continue mindfully"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (relapseState.isEscalated) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (relapseState.isEscalated) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                DecisionStep.CHOOSE_DURATION -> {
                    Text(
                        text = "Set Intended Time",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.2).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (relapseState.isEscalated) "Relapse session: -5 mindful points will be deducted" else "A gentle nudge will sound when time is up",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (relapseState.isEscalated) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Readout badge
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$selectedMinutes",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedMinutes == 1) "minute session" else "minutes session",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Slider
                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            val rounded = kotlin.math.round(newValue).toInt().coerceIn(1, 60)
                            if (rounded != selectedMinutes) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedMinutes = rounded
                            }
                            sliderValue = newValue
                        },
                        valueRange = 1f..60f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 5, 10, 15, 30).forEach { preset ->
                            val isSelected = selectedMinutes == preset
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    Color.White.copy(alpha = 0.04f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        selectedMinutes = preset
                                        sliderValue = preset.toFloat()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${preset}m",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Enter Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onOpenSession(selectedMinutes, relapseState.isEscalated)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (relapseState.isEscalated) {
                                Color(0xFFD84315)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 54.dp)
                    ) {
                        Text(
                            text = if (isSoftNudge) {
                                "Continue for $selectedMinutes min"
                            } else if (relapseState.isEscalated) {
                                "Enter for $selectedMinutes min (-5 pts)"
                            } else {
                                "Enter for $selectedMinutes min"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Back / Walk away
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            currentStep = DecisionStep.ASK_ENTER
                        }
                    ) {
                        Text(
                            text = "← Changed my mind (Walk away)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelapseFrictionBanner(
    state: RelapseState,
    remainingSeconds: Int
) {
    val isSevere = state.tier >= 3
    val containerColor = if (isSevere) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
    } else {
        Color(0xFFFFA726).copy(alpha = 0.18f)
    }
    val contentColor = if (isSevere) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFFFFB74D)
    }
    val borderColor = contentColor.copy(alpha = 0.4f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (remainingSeconds > 0) Icons.Default.Timer else Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSevere) {
                        "⚠️ Relapse Alert • Open #${state.recentAttemptsCount} in 15m"
                    } else {
                        "⚠️ Relapse Friction • Open #${state.recentAttemptsCount} in 15m"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                )
                Text(
                    text = if (remainingSeconds > 0) {
                        "Mindful pause active: ${remainingSeconds}s remaining"
                    } else {
                        "Proceeding will deduct 5 mindful points"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = contentColor.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}

/**
 * Triggers a serene mindful alert (calm harmonic chime + gentle rhythmic vibration)
 * to peacefully signal when the chosen session duration has ended.
 */
private fun triggerMindfulSessionCompleteAlert(context: Context) {
    // 1. Soothing haptic vibration pattern
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Gentle calm double-pulse pattern
                val timings = longArrayOf(0, 180, 120, 240)
                val amplitudes = intArrayOf(0, 140, 0, 190)
                it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 180, 120, 240), -1)
            }
        }
    } catch (_: Exception) {}

    // 2. Serene mindful singing bowl audio chime (528 Hz harmonic chime)
    try {
        val sampleRate = 44100
        val durationSeconds = 1.2
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val baseFreq = 528.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = kotlin.math.exp(-3.2 * t)
            val sample = (kotlin.math.sin(2.0 * Math.PI * baseFreq * t) +
                    0.35 * kotlin.math.sin(2.0 * Math.PI * (baseFreq * 2.0) * t) +
                    0.15 * kotlin.math.sin(2.0 * Math.PI * (baseFreq * 3.0) * t)) * envelope
            buffer[i] = (sample * 16000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
    } catch (_: Exception) {}
}
