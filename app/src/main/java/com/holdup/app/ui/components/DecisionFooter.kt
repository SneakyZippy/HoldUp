package com.holdup.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.data.model.AlternativeActivity
import com.holdup.app.data.model.RelapseState
import com.holdup.app.data.model.SessionPenaltyCalculator

enum class DecisionStep {
    ASK_ENTER,
    CHOOSE_DURATION
}

@Composable
fun DecisionFooter(
    isSoftNudge: Boolean,
    targetPackageName: String,
    maxSessionMinutes: Int = 15,
    graceMinutesRemaining: Int = 0,
    relapseState: RelapseState = RelapseState(),
    selectedAlternative: AlternativeActivity?,
    onWalkAway: () -> Unit,
    onOpenSession: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

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
    val defaultMins = if (maxSessionMinutes in 1..60) maxSessionMinutes else 10
    val initialMins = if (graceMinutesRemaining > 0 && !relapseState.isEscalated) {
        minOf(defaultMins, graceMinutesRemaining).coerceAtLeast(1)
    } else {
        defaultMins
    }
    var selectedMinutes by remember(initialMins) { mutableIntStateOf(initialMins) }
    var sliderValue by remember(initialMins) { mutableFloatStateOf(initialMins.toFloat()) }

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
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
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
                DecisionStep.ASK_ENTER -> AskEnterStep(
                    isSoftNudge = isSoftNudge,
                    appName = appName,
                    selectedMinutes = selectedMinutes,
                    graceMinutesRemaining = graceMinutesRemaining,
                    relapseState = relapseState,
                    selectedAlternative = selectedAlternative,
                    onWalkAway = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onWalkAway()
                    },
                    onProceedToDuration = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentStep = DecisionStep.CHOOSE_DURATION
                    }
                )
                DecisionStep.CHOOSE_DURATION -> ChooseDurationStep(
                    isSoftNudge = isSoftNudge,
                    selectedMinutes = selectedMinutes,
                    sliderValue = sliderValue,
                    graceMinutesRemaining = graceMinutesRemaining,
                    relapseState = relapseState,
                    onDurationChange = { minutes, rawSlider, triggerHaptic ->
                        if (triggerHaptic) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        selectedMinutes = minutes
                        sliderValue = rawSlider
                    },
                    onEnterSession = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSession(selectedMinutes)
                    },
                    onBack = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentStep = DecisionStep.ASK_ENTER
                    }
                )
            }
        }
    }
}

@Composable
private fun AskEnterStep(
    isSoftNudge: Boolean,
    appName: String,
    selectedMinutes: Int,
    graceMinutesRemaining: Int,
    relapseState: RelapseState,
    selectedAlternative: AlternativeActivity?,
    onWalkAway: () -> Unit,
    onProceedToDuration: () -> Unit
) {
    val canUseGrace = graceMinutesRemaining > 0 && !relapseState.isEscalated
    val defaultCost = SessionPenaltyCalculator.calculateCost(
        plannedMinutes = selectedMinutes,
        graceMinutesRemaining = graceMinutesRemaining,
        relapseState = relapseState
    )

    Text(
        text = if (isSoftNudge) "Ready to step back?" else "Still want to enter?",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            letterSpacing = (-0.2).sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = when {
            isSoftNudge -> "Your planned session has ended. Step away for clarity."
            selectedAlternative != null -> "Selected: ${selectedAlternative.title}"
            else -> "Give yourself the gift of intentional presence."
        },
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onWalkAway,
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedAlternative?.title ?: "Walk Away",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onProceedToDuration)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = when {
                    relapseState.isEscalated && appName.isNotBlank() ->
                        "Open $appName mindfully (-${defaultCost.totalPenaltyPoints} pts)"
                    relapseState.isEscalated ->
                        "Continue mindfully (-${defaultCost.totalPenaltyPoints} pts)"
                    canUseGrace && isSoftNudge -> "Use Grace to continue (${graceMinutesRemaining}m left)"
                    canUseGrace && appName.isNotBlank() -> "Use Grace on $appName (${graceMinutesRemaining}m left)"
                    canUseGrace -> "Use Grace (${graceMinutesRemaining}m left)"
                    isSoftNudge -> "Continue (-${defaultCost.totalPenaltyPoints} pts • resets Clean Focus)"
                    appName.isNotBlank() -> "Open $appName (-${defaultCost.totalPenaltyPoints} pts • resets Clean Focus)"
                    else -> "Continue (-${defaultCost.totalPenaltyPoints} pts • resets Clean Focus)"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (relapseState.isEscalated) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (relapseState.isEscalated) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
