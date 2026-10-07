package com.holdup.app.data.model

import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
enum class SchedulePreset(val displayName: String, val subtitle: String) {
    ALL_DAY("24/7", "Always active"),
    WORK_HOURS("Work (9–17)", "Mon–Fri only"),
    BEDTIME("Night (22–7)", "Bedtime protection")
}

@Serializable
data class AppRuleConfig(
    val packageName: String,
    val ruleMode: RuleMode = RuleMode.SHUFFLE,
    val specificType: InterventionType = InterventionType.BREATHING,
    val breathingSeconds: Int = 8,
    val countdownSeconds: Int = 10,
    val schedulePreset: SchedulePreset = SchedulePreset.ALL_DAY,
    val maxSessionMinutes: Int = 15,
    val customPhotoUri: String? = null,
    val customPhotoCaption: String = "Remember what truly matters to you.",
    val customVideoUri: String? = null
)

fun AppRuleConfig.isCurrentlyActive(): Boolean {
    if (schedulePreset == SchedulePreset.ALL_DAY) return true
    val cal = Calendar.getInstance()
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = cal.get(Calendar.MINUTE)
    val currentTotal = currentHour * 60 + currentMinute

    val (sH, sM, eH, eM) = when (schedulePreset) {
        SchedulePreset.WORK_HOURS -> {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return false
            listOf(9, 0, 17, 0)
        }
        SchedulePreset.BEDTIME -> listOf(22, 0, 7, 0)
        SchedulePreset.ALL_DAY -> listOf(0, 0, 23, 59)
    }

    val startTotal = sH * 60 + sM
    val endTotal = eH * 60 + eM

    return if (startTotal <= endTotal) {
        currentTotal in startTotal..endTotal
    } else {
        // Overnight wrap-around (e.g. 22:00 to 07:00)
        currentTotal >= startTotal || currentTotal <= endTotal
    }
}

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
