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
                            scope.saveLastChosenDuration(targetPackage, minutes)
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
        val rememberedDuration = prefs.getLastChosenDuration(targetPackageName)
        maxSessionMinutes = rememberedDuration ?: rule?.maxSessionMinutes ?: global.defaultSessionMinutes

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

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(isSoftNudge) {
        if (isSoftNudge) {
            triggerMindfulSessionCompleteAlert(context)
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
                    targetPackageName = targetPackageName,
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

enum class DecisionStep {
    ASK_ENTER,
    CHOOSE_DURATION
}

@Composable
fun DecisionFooter(
    isSoftNudge: Boolean,
    targetPackageName: String,
    maxSessionMinutes: Int = 15,
    selectedAlternative: AlternativeActivity?,
    onWalkAway: () -> Unit,
    onOpenSession: (Int) -> Unit
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                DecisionStep.ASK_ENTER -> {
                    Text(
                        text = if (isSoftNudge) "Do you really want to continue?" else "Do you really want to enter?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isSoftNudge) {
                            "Your session ended. Pause and decide if you want to keep going."
                        } else if (selectedAlternative != null) {
                            "You picked: ${selectedAlternative.title}"
                        } else {
                            "Pause and ask yourself if this is intentional."
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
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedAlternative != null) {
                                "${selectedAlternative.title} (+20 pts)"
                            } else {
                                "No, Walk Away (+10 pts)"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Choice: Yes -> advances to slider
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            currentStep = DecisionStep.CHOOSE_DURATION
                        },
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (appName.isNotBlank()) "Yes, enter $appName" else "Yes",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                DecisionStep.CHOOSE_DURATION -> {
                    Text(
                        text = "How long?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Choose how long it should go without a break",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Readout badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
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
                                text = if (selectedMinutes == 1) "min without a break" else "mins without a break",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
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
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${preset}m",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
                            onOpenSession(selectedMinutes)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (isSoftNudge) "Continue for $selectedMinutes min" else "Enter for $selectedMinutes min",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
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
