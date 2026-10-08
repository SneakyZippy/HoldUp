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
    val ruleMode: RuleMode = RuleMode.ROTATE,
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
    val defaultRuleMode: RuleMode = RuleMode.ROTATE,
    val defaultBreathingSeconds: Int = 8,
    val defaultCountdownSeconds: Int = 10,
    val lovedOnePhotoUri: String? = null,
    val lovedOneCaption: String = "Take a breath. Is this how you want to spend this moment?",
    val friendVideoUri: String? = null,
    val defaultSessionMinutes: Int = 5,
    val enabledInterventions: List<InterventionType> = listOf(
        InterventionType.BREATHING,
        InterventionType.REFLECTION,
        InterventionType.PHOTO,
        InterventionType.VIDEO,
        InterventionType.ALTERNATIVES
    )
)

val defaultReflections: List<String> = listOf(
    "Are you opening this out of intention, or habit?",
    "Take a slow, deep breath. Notice how your body feels right now.",
    "The real world is waiting for you outside this screen.",
    "Is this how you want to spend the next 20 minutes?",
    "Boredom is just space for your own creativity.",
    "You are in control of your attention. Where does it belong right now?",
    "What were you doing just before you picked up your phone?",
    "A minute of stillness is worth hours of scrolling.",
    "Notice the urge to scroll. Can you let it pass like a wave?",
    "Whatever you're looking for, you won't find it at the bottom of a feed.",
    "Your time is your life. Spend it on things you'll remember.",
    "Pause for three seconds. Relax your shoulders and unclench your jaw.",
    "Scrolling numbs discomfort, but presence creates joy.",
    "Ask yourself: What do I actually need in this moment?",
    "Look around you. Name three things you can see right now.",
    "Almost nothing on this app will matter to you tomorrow.",
    "Choose creation over consumption today.",
    "You don't need to be entertained every second.",
    "The algorithm is designed to hold you. You have the power to put it down.",
    "Honor the goals you set for yourself earlier today.",
    "Be present with what is, rather than escaping into what isn't.",
    "Give yourself permission to do nothing for sixty seconds.",
    "Peace of mind starts with protecting your focus.",
    "Is this adding value to your life, or just filling the silence?",
    "Every time you walk away, you strengthen your focus muscle.",
    "One mindful choice right now can change the trajectory of your entire day.",
    "The best moments of your life won't happen inside an app."
)

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isMonitored: Boolean = false,
    val ruleConfig: AppRuleConfig? = null
)
