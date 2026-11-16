package com.holdup.app.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionPenaltyCalculatorTest {

    private fun sampleRecord(
        plannedMinutes: Int,
        graceMinutesCharged: Int,
        baseRelapsePenaltyPoints: Int = 0,
        todayPointsDeducted: Int = 0,
        lifetimePointsDeducted: Int = 0,
        spendablePointsDeducted: Int = 0
    ) = ActiveSessionRecord(
        packageName = "com.instagram.android",
        dateString = "2026-11-16",
        plannedMinutes = plannedMinutes,
        graceMinutesCharged = graceMinutesCharged,
        baseRelapsePenaltyPoints = baseRelapsePenaltyPoints,
        todayPointsDeducted = todayPointsDeducted,
        lifetimePointsDeducted = lifetimePointsDeducted,
        spendablePointsDeducted = spendablePointsDeducted,
        startedAtMillis = 1_000_000L,
        expiresAtMillis = 1_000_000L + plannedMinutes * 60_000L
    )

    @Test
    fun `within grace session charges zero points and refunds unused grace on early close`() {
        val cost = SessionPenaltyCalculator.calculateCost(
            plannedMinutes = 10,
            graceMinutesRemaining = 15,
            relapseState = RelapseState(tier = 1, penaltyPoints = 0)
        )
        assertEquals(10, cost.graceMinutesUsed)
        assertEquals(0, cost.overBudgetMinutes)
        assertEquals(0, cost.totalPenaltyPoints)
        assertTrue(cost.usesGrace)
        assertFalse(cost.exceedsGrace)

        val record = sampleRecord(plannedMinutes = 10, graceMinutesCharged = 10)
        val refundAfter2Min = SessionPenaltyCalculator.calculateRefund(record, activeDurationMs = 120_000L)
        assertEquals(2, refundAfter2Min.actualSpentMinutes)
        assertEquals(8, refundAfter2Min.graceMinutesRefunded)
        assertEquals(0, refundAfter2Min.spendablePointsRefunded)
        assertTrue(refundAfter2Min.hasRefund)
    }

    @Test
    fun `after grace sessions scale penalty by duration and refund unused stay points`() {
        val shortCost = SessionPenaltyCalculator.calculateCost(plannedMinutes = 2, graceMinutesRemaining = 0)
        val mediumCost = SessionPenaltyCalculator.calculateCost(plannedMinutes = 10, graceMinutesRemaining = 0)
        val longCost = SessionPenaltyCalculator.calculateCost(plannedMinutes = 30, graceMinutesRemaining = 0)

        assertEquals(2, shortCost.totalPenaltyPoints)
        assertEquals(10, mediumCost.totalPenaltyPoints)
        assertEquals(30, longCost.totalPenaltyPoints)
        assertTrue(longCost.totalPenaltyPoints > mediumCost.totalPenaltyPoints)
        assertTrue(mediumCost.totalPenaltyPoints > shortCost.totalPenaltyPoints)

        val record = sampleRecord(
            plannedMinutes = 10,
            graceMinutesCharged = 0,
            todayPointsDeducted = 10,
            lifetimePointsDeducted = 10,
            spendablePointsDeducted = 10
        )
        val refundAfter2Min = SessionPenaltyCalculator.calculateRefund(record, activeDurationMs = 120_000L)
        assertEquals(2, refundAfter2Min.actualSpentMinutes)
        assertEquals(0, refundAfter2Min.graceMinutesRefunded)
        assertEquals(8, refundAfter2Min.todayPointsRefunded)
        assertEquals(8, refundAfter2Min.lifetimePointsRefunded)
        assertEquals(8, refundAfter2Min.spendablePointsRefunded)
    }

    @Test
    fun `partial grace session consumes remaining grace first then charges over-budget minutes`() {
        val cost = SessionPenaltyCalculator.calculateCost(plannedMinutes = 10, graceMinutesRemaining = 5)
        assertEquals(5, cost.graceMinutesUsed)
        assertEquals(5, cost.overBudgetMinutes)
        assertEquals(5, cost.durationPenaltyPoints)
        assertEquals(5, cost.totalPenaltyPoints)
        assertTrue(cost.usesGrace)
        assertTrue(cost.exceedsGrace)

        val record = sampleRecord(
            plannedMinutes = 10,
            graceMinutesCharged = 5,
            todayPointsDeducted = 5,
            lifetimePointsDeducted = 5,
            spendablePointsDeducted = 5
        )

        val refundAt2Min = SessionPenaltyCalculator.calculateRefund(record, activeDurationMs = 120_000L)
        assertEquals(2, refundAt2Min.actualSpentMinutes)
        assertEquals(3, refundAt2Min.graceMinutesRefunded)
        assertEquals(5, refundAt2Min.spendablePointsRefunded)

        val refundAt7Min = SessionPenaltyCalculator.calculateRefund(record, activeDurationMs = 420_000L)
        assertEquals(7, refundAt7Min.actualSpentMinutes)
        assertEquals(0, refundAt7Min.graceMinutesRefunded)
        assertEquals(3, refundAt7Min.spendablePointsRefunded)
    }

    @Test
    fun `escalated relapse disallows grace and never refunds base relapse penalty`() {
        val relapse = RelapseState(tier = 2, recentAttemptsCount = 2, delaySeconds = 10, penaltyPoints = 5)
        val cost = SessionPenaltyCalculator.calculateCost(
            plannedMinutes = 10,
            graceMinutesRemaining = 15,
            relapseState = relapse
        )

        assertEquals(0, cost.graceMinutesUsed)
        assertEquals(10, cost.overBudgetMinutes)
        assertEquals(5, cost.baseRelapsePenaltyPoints)
        assertEquals(10, cost.durationPenaltyPoints)
        assertEquals(15, cost.totalPenaltyPoints)
        assertTrue(cost.isEscalated)

        val record = sampleRecord(
            plannedMinutes = 10,
            graceMinutesCharged = 0,
            baseRelapsePenaltyPoints = 5,
            todayPointsDeducted = 15,
            lifetimePointsDeducted = 15,
            spendablePointsDeducted = 15
        )
        val refundAt2Min = SessionPenaltyCalculator.calculateRefund(record, activeDurationMs = 120_000L)
        assertEquals(2, refundAt2Min.actualSpentMinutes)
        // Actual penalty = 5 base + 2 duration = 7, so refund is 15 - 7 = 8 (never refunds base 5 pts)
        assertEquals(8, refundAt2Min.spendablePointsRefunded)
        assertEquals(8, refundAt2Min.lifetimePointsRefunded)
    }

    @Test
    fun `clamped balance refund never grants net points on early exit`() {
        // User had only 3 spendable points (and 0 today points), opened a 10m over-budget session
        val record = sampleRecord(
            plannedMinutes = 10,
            graceMinutesCharged = 0,
            baseRelapsePenaltyPoints = 0,
            todayPointsDeducted = 0,
            lifetimePointsDeducted = 10,
            spendablePointsDeducted = 3
        )

        // Closing after 2m means 2 pts were owed; only 1 of the 3 deducted spendable pts is refunded
        val refund = SessionPenaltyCalculator.calculateRefund(record, activeDurationMs = 120_000L)
        assertEquals(0, refund.todayPointsRefunded)
        assertEquals(8, refund.lifetimePointsRefunded)
        assertEquals(1, refund.spendablePointsRefunded)
    }

    @Test
    fun `actual spent minutes enforces 1m floor and planned ceiling`() {
        assertEquals(1, SessionPenaltyCalculator.calculateActualSpentMinutes(activeDurationMs = 5_000L, plannedMinutes = 10))
        assertEquals(2, SessionPenaltyCalculator.calculateActualSpentMinutes(activeDurationMs = 120_000L, plannedMinutes = 10))
        assertEquals(10, SessionPenaltyCalculator.calculateActualSpentMinutes(activeDurationMs = 900_000L, plannedMinutes = 10))

        val fullStayRecord = sampleRecord(plannedMinutes = 10, graceMinutesCharged = 10)
        val noRefund = SessionPenaltyCalculator.calculateRefund(fullStayRecord, activeDurationMs = 600_000L)
        assertFalse(noRefund.hasRefund)
    }
}
