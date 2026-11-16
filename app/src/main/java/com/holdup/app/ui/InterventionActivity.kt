package com.holdup.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holdup.app.HoldUpApp
import com.holdup.app.data.model.*
import com.holdup.app.service.HoldUpAccessibilityService
import com.holdup.app.service.SessionMonitorService
import com.holdup.app.ui.components.*
import com.holdup.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                            Toast.makeText(
                                this@InterventionActivity,
                                "Test complete: Walk Away",
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
                    onOpenSession = { minutes, cost ->
                        isInterventionActive = false
                        activeTargetPackage = null
                        if (targetPackage.isNotBlank()) {
                            HoldUpAccessibilityService.suppressInterception(targetPackage, 2500L)
                        }
                        if (isTestMode) {
                            val graceNote = if (cost.usesGrace) " (${cost.graceMinutesUsed}m Grace)" else ""
                            val penaltyNote = if (cost.totalPenaltyPoints > 0) " (-${cost.totalPenaltyPoints} pts)" else ""
                            val msg = "Test complete: ${minutes}m session$graceNote$penaltyNote"
                            Toast.makeText(this@InterventionActivity, msg, Toast.LENGTH_SHORT).show()
                            finishAndRemoveTask()
                            return@InterventionScreen
                        }
                        val app = application as HoldUpApp
                        val appContext = applicationContext
                        val relapsePenalty = if (cost.isEscalated) cost.baseRelapsePenaltyPoints else 0
                        app.applicationScope.launch {
                            val recordedCost = app.preferencesManager.recordProceedSession(
                                packageName = targetPackage,
                                durationMinutes = minutes,
                                relapsePenaltyPoints = relapsePenalty
                            )
                            app.preferencesManager.saveLastChosenDuration(targetPackage, minutes)
                            if (recordedCost.totalPenaltyPoints > 0) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(
                                        appContext,
                                        "⚠️ ${recordedCost.totalPenaltyPoints} mindful points deducted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
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
    onOpenSession: (Int, SessionCostBreakdown) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val prefs = HoldUpApp.instance.preferencesManager
    val zooState by prefs.zooState.collectAsStateWithLifecycle(initialValue = ZooState())
    val todayStats by prefs.todayStats.collectAsStateWithLifecycle(initialValue = DailyStats())

    var isConfigLoaded by remember { mutableStateOf(false) }
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
    var graceMinutesRemaining by remember { mutableIntStateOf(0) }

    var relapseState by remember { mutableStateOf(RelapseState()) }
    var relapseRemainingSeconds by remember { mutableIntStateOf(0) }
    var isInterventionCompleted by remember { mutableStateOf(isSoftNudge) }
    var currentQuote by remember { mutableStateOf("") }
    var availableQuotes by remember { mutableStateOf<List<String>>(defaultReflections) }

    val scrollState = rememberScrollState()

    LaunchedEffect(isInterventionCompleted) {
        if (isInterventionCompleted) {
            kotlinx.coroutines.delay(120L)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Countdown effect for Relapse Friction delay banner
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
        graceMinutesRemaining = prefs.todayStats.first().graceMinutesRemaining

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

        // Configuration is fully loaded and stabilized before starting timer
        isConfigLoaded = true
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

    val isBreathingActive = activeInterventionType == InterventionType.BREATHING && !isSoftNudge
    val atmosphereColor = when {
        relapseState.isEscalated -> ZenAmber
        activeInterventionType == InterventionType.REFLECTION -> ZenLavender
        activeInterventionType == InterventionType.PHOTO || activeInterventionType == InterventionType.VIDEO -> ZenCoral
        else -> MaterialTheme.colorScheme.primary
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "AtmospherePulse")
        val atmosphereAlpha by infiniteTransition.animateFloat(
            initialValue = if (isBreathingActive) 0.08f else 0.05f,
            targetValue = if (isBreathingActive) 0.22f else 0.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (isBreathingActive) 4000 else 6000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "AtmosphereAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            atmosphereColor.copy(alpha = atmosphereAlpha),
                            MaterialTheme.colorScheme.secondary.copy(alpha = atmosphereAlpha * 0.35f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .verticalScroll(scrollState)
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
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Preview • ${activeInterventionType.displayName}",
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
                        val effectiveFrictionSeconds = remember(breathingSeconds, relapseState.delaySeconds) {
                            maxOf(breathingSeconds, relapseState.delaySeconds)
                        }

                        when (activeInterventionType) {
                            InterventionType.BREATHING -> {
                                BreathingOrb(
                                    totalSeconds = effectiveFrictionSeconds,
                                    isReady = isConfigLoaded,
                                    onCompleted = {
                                        isInterventionCompleted = true
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
                enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 30 }),
                exit = fadeOut(tween(200))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (activeInterventionType == InterventionType.BREATHING && !isSoftNudge) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mindful Pause Complete",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.3.sp
                                    )
                                )
                            }
                        }
                    }
                    InterventionPetBanner(
                        activePet = zooState.activePet,
                        todayStats = todayStats,
                        graceMinutesRemaining = graceMinutesRemaining,
                        isEscalated = relapseState.isEscalated
                    )
                    DecisionFooter(
                        isSoftNudge = isSoftNudge,
                        targetPackageName = targetPackageName,
                        maxSessionMinutes = maxSessionMinutes,
                        graceMinutesRemaining = graceMinutesRemaining,
                        relapseState = relapseState,
                        selectedAlternative = selectedAlternative,
                        onWalkAway = { onWalkAway(selectedAlternative != null) },
                        onOpenSession = { minutes ->
                            val cost = SessionPenaltyCalculator.calculateCost(
                                plannedMinutes = minutes,
                                graceMinutesRemaining = graceMinutesRemaining,
                                relapseState = relapseState
                            )
                            onOpenSession(minutes, cost)
                        }
                    )
                }
            }
        }
    }
}
