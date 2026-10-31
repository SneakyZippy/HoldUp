package com.holdup.app.data.model

import kotlinx.serialization.Serializable

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
    val isCleanDayBonusAwarded: Boolean = false
) {
    val successRatePercent: Int
        get() = if (totalInterceptions > 0) {
            ((walkedAwayCount.toFloat() / totalInterceptions.toFloat()) * 100).toInt()
        } else {
            100
        }

    val cleanDayBonusStatus: String
        get() = when {
            totalInterceptions == 0 -> "Pure Zen (+50 pts)"
            totalInterceptions in 1..2 && proceededCount == 0 -> "Mindful Recovery (+25 pts)"
            else -> "Interrupted"
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
