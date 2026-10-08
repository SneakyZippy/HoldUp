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
    val lifetimePoints: Int = 0
) {
    val successRatePercent: Int
        get() = if (totalInterceptions > 0) {
            ((walkedAwayCount.toFloat() / totalInterceptions.toFloat()) * 100).toInt()
        } else {
            100
        }

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
