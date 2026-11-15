package com.holdup.app.data.model

import kotlinx.serialization.Serializable

/**
 * Selectable daily grace budgets. Stricter budgets award higher Clean Focus and Clean Day points.
 */
enum class GraceBudgetPreset(
    val minutes: Int,
    val label: String,
    val multiplierLabel: String,
    val hourlyPoints: Int,
    val cleanDayBonus: Int
) {
    STRICT(minutes = 0, label = "Off (0m)", multiplierLabel = "2.0×", hourlyPoints = 20, cleanDayBonus = 80),
    MINIMAL(minutes = 5, label = "5m", multiplierLabel = "1.5×", hourlyPoints = 15, cleanDayBonus = 60),
    BALANCED(minutes = 15, label = "15m", multiplierLabel = "1.0×", hourlyPoints = 10, cleanDayBonus = 40),
    FLEXIBLE(minutes = 30, label = "30m", multiplierLabel = "0.7×", hourlyPoints = 7, cleanDayBonus = 25),
    LIGHT(minutes = 60, label = "1h", multiplierLabel = "0.4×", hourlyPoints = 4, cleanDayBonus = 15);

    companion object {
        fun fromMinutes(minutes: Int): GraceBudgetPreset {
            return entries.minByOrNull { kotlin.math.abs(it.minutes - minutes) } ?: BALANCED
        }
    }
}

enum class StreakMilestone(
    val days: Int,
    val title: String,
    val subtitle: String
) {
    SPARK(days = 3, title = "3-Day Spark", subtitle = "3 mindful days in a row"),
    STEADY(days = 7, title = "7-Day Steady", subtitle = "A full week within grace"),
    GROUNDED(days = 14, title = "14-Day Grounded", subtitle = "Two weeks of intention"),
    UNSHAKEABLE(days = 30, title = "30-Day Unshakeable", subtitle = "A full month of mastery")
}

@Serializable
data class DailyStats(
    val dateString: String = "",
    val totalInterceptions: Int = 0,
    val walkedAwayCount: Int = 0,
    val proceededCount: Int = 0,
    val estimatedMinutesSaved: Int = 0,
    val pointsEarnedToday: Int = 0,
    val lifetimePoints: Int = 0,
    val cleanFocusMinutesToday: Int = 0,
    val cleanFocusHoursAwarded: Int = 0,
    val isCleanDayBonusAwarded: Boolean = false,
    val dailyGraceBudgetMinutes: Int = 15,
    val graceMinutesUsedToday: Int = 0,
    val overBudgetOpensToday: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val streakFreezesOwned: Int = 0
) {
    val gracePreset: GraceBudgetPreset
        get() = GraceBudgetPreset.fromMinutes(dailyGraceBudgetMinutes)

    val graceMinutesRemaining: Int
        get() = (dailyGraceBudgetMinutes - graceMinutesUsedToday).coerceAtLeast(0)

    val isWithinGraceToday: Boolean
        get() = overBudgetOpensToday == 0

    val unlockedMilestones: List<StreakMilestone>
        get() = StreakMilestone.entries.filter { longestStreakDays >= it.days || currentStreakDays >= it.days }

    val successRatePercent: Int
        get() = if (totalInterceptions > 0) {
            ((walkedAwayCount.toFloat() / totalInterceptions.toFloat()) * 100).toInt()
        } else {
            100
        }

    val cleanDayBonusStatus: String
        get() {
            val bonus = gracePreset.cleanDayBonus
            return when {
                overBudgetOpensToday == 0 && graceMinutesUsedToday == 0 -> "Pure Zen (+$bonus pts)"
                overBudgetOpensToday == 0 -> "Within Grace (+${(bonus / 2).coerceAtLeast(5)} pts)"
                else -> "Grace Exceeded"
            }
        }

    val currentCleanStreakHours: Int
        get() = cleanFocusMinutesToday / 60

    val currentCleanStreakMinutesPart: Int
        get() = cleanFocusMinutesToday % 60

    val minutesToNextFocusPoint: Int
        get() = (60 - (cleanFocusMinutesToday % 60)).coerceIn(1, 60)

    val focusHourProgress: Float
        get() = ((cleanFocusMinutesToday % 60).toFloat() / 60f).coerceIn(0f, 1f)

    val userLevel: String
        get() = when {
            lifetimePoints >= 1000 -> "Zen Master"
            lifetimePoints >= 600 -> "Habit Champion"
            lifetimePoints >= 300 -> "Focus Pro"
            lifetimePoints >= 100 -> "Focus Builder"
            else -> "Mindful Novice"
        }

    val currentLevelNumber: Int
        get() = when {
            lifetimePoints >= 1000 -> 5
            lifetimePoints >= 600 -> 4
            lifetimePoints >= 300 -> 3
            lifetimePoints >= 100 -> 2
            else -> 1
        }

    val nextLevelTarget: Int
        get() = when {
            lifetimePoints >= 1000 -> 2000
            lifetimePoints >= 600 -> 1000
            lifetimePoints >= 300 -> 600
            lifetimePoints >= 100 -> 300
            else -> 100
        }
}

@Serializable
data class RelapseState(
    val tier: Int = 1,
    val recentAttemptsCount: Int = 1,
    val delaySeconds: Int = 0,
    val penaltyPoints: Int = 0
) {
    val isEscalated: Boolean get() = tier >= 2
}
