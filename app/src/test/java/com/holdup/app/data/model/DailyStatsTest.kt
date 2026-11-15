package com.holdup.app.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyStatsTest {

    @Test
    fun `stricter grace budget earns higher hourly and clean day points`() {
        val ordered = listOf(
            GraceBudgetPreset.STRICT,
            GraceBudgetPreset.MINIMAL,
            GraceBudgetPreset.BALANCED,
            GraceBudgetPreset.FLEXIBLE,
            GraceBudgetPreset.LIGHT
        )

        for (i in 0 until ordered.lastIndex) {
            assertTrue(ordered[i].hourlyPoints > ordered[i + 1].hourlyPoints)
            assertTrue(ordered[i].cleanDayBonus > ordered[i + 1].cleanDayBonus)
        }
    }

    @Test
    fun `grace remaining and within grace status reflect usage`() {
        val within = DailyStats(
            dailyGraceBudgetMinutes = 15,
            graceMinutesUsedToday = 10,
            overBudgetOpensToday = 0
        )
        assertEquals(5, within.graceMinutesRemaining)
        assertTrue(within.isWithinGraceToday)
        assertTrue(within.cleanDayBonusStatus.contains("Within Grace"))

        val exceeded = within.copy(graceMinutesUsedToday = 20, overBudgetOpensToday = 1)
        assertEquals(0, exceeded.graceMinutesRemaining)
        assertFalse(exceeded.isWithinGraceToday)
        assertEquals("Grace Exceeded", exceeded.cleanDayBonusStatus)
    }

    @Test
    fun `unlocked milestones match current and longest streak`() {
        val stats = DailyStats(currentStreakDays = 7, longestStreakDays = 14)
        assertEquals(
            listOf(StreakMilestone.SPARK, StreakMilestone.STEADY, StreakMilestone.GROUNDED),
            stats.unlockedMilestones
        )
    }
}
