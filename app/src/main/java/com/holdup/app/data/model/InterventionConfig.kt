package com.holdup.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppRuleConfig(
    val packageName: String,
    val ruleMode: RuleMode = RuleMode.SHUFFLE,
    val specificType: InterventionType = InterventionType.BREATHING,
    val breathingSeconds: Int = 8,
    val countdownSeconds: Int = 10,
    val customPhotoUri: String? = null,
    val customPhotoCaption: String = "Remember what truly matters to you.",
    val customVideoUri: String? = null
)

@Serializable
data class GlobalInterventionSettings(
    val defaultBreathingSeconds: Int = 8,
    val defaultCountdownSeconds: Int = 10,
    val lovedOnePhotoUri: String? = null,
    val lovedOneCaption: String = "Take a breath. Is this how you want to spend this moment?",
    val friendVideoUri: String? = null,
    val defaultSessionMinutes: Int = 5,
    val enabledInterventions: List<InterventionType> = listOf(
        InterventionType.BREATHING,
        InterventionType.PHOTO,
        InterventionType.COUNTDOWN,
        InterventionType.ALTERNATIVES
    )
)

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isMonitored: Boolean = false,
    val ruleConfig: AppRuleConfig? = null
)
