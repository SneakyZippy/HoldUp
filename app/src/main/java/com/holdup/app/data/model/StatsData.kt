package com.holdup.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyStats(
    val dateString: String = "",
    val totalInterceptions: Int = 0,
    val walkedAwayCount: Int = 0,
    val proceededCount: Int = 0,
    val estimatedMinutesSaved: Int = 0
) {
    val successRatePercent: Int
        get() = if (totalInterceptions > 0) {
            ((walkedAwayCount.toFloat() / totalInterceptions.toFloat()) * 100).toInt()
        } else {
            100
        }
}
