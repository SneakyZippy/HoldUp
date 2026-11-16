package com.holdup.app.data.model

import kotlinx.serialization.Serializable

/**
 * Breakdown of Daily Grace minutes and mindful points charged when starting an app session.
 */
data class SessionCostBreakdown(
    val plannedMinutes: Int,
    val graceMinutesUsed: Int,
    val overBudgetMinutes: Int,
    val baseRelapsePenaltyPoints: Int,
    val durationPenaltyPoints: Int,
    val totalPenaltyPoints: Int,
    val isEscalated: Boolean
) {
    val usesGrace: Boolean get() = graceMinutesUsed > 0
    val exceedsGrace: Boolean get() = overBudgetMinutes > 0 || isEscalated
}

/**
 * Persisted snapshot of what was actually deducted when a session started, used to compute
 * safe early-exit refunds without ever granting net points when balances were clamped at zero.
 */
@Serializable
data class ActiveSessionRecord(
    val packageName: String,
    val dateString: String,
    val plannedMinutes: Int,
    val graceMinutesCharged: Int,
    val baseRelapsePenaltyPoints: Int,
    val todayPointsDeducted: Int,
    val lifetimePointsDeducted: Int,
    val spendablePointsDeducted: Int,
    val startedAtMillis: Long,
    val expiresAtMillis: Long
)

/**
 * Computed Grace minutes and point refunds when a user leaves a session before its planned end.
 */
data class SessionRefundResult(
    val actualSpentMinutes: Int,
    val graceMinutesRefunded: Int,
    val todayPointsRefunded: Int,
    val lifetimePointsRefunded: Int,
    val spendablePointsRefunded: Int
) {
    val hasRefund: Boolean
        get() = graceMinutesRefunded > 0 ||
            todayPointsRefunded > 0 ||
            lifetimePointsRefunded > 0 ||
            spendablePointsRefunded > 0
}

/**
 * Computes duration-scaled point penalties beyond Daily Grace and proportional refunds on early exit.
 */
object SessionPenaltyCalculator {
    const val POINTS_PER_OVER_BUDGET_MINUTE = 1

    fun calculateCost(
        plannedMinutes: Int,
        graceMinutesRemaining: Int,
        relapseState: RelapseState = RelapseState()
    ): SessionCostBreakdown = calculateCost(
        plannedMinutes = plannedMinutes,
        graceMinutesRemaining = graceMinutesRemaining,
        isEscalated = relapseState.isEscalated,
        relapsePenaltyPoints = relapseState.penaltyPoints
    )

    fun calculateCost(
        plannedMinutes: Int,
        graceMinutesRemaining: Int,
        isEscalated: Boolean,
        relapsePenaltyPoints: Int
    ): SessionCostBreakdown {
        val safePlanned = plannedMinutes.coerceAtLeast(1)
        val safeGrace = graceMinutesRemaining.coerceAtLeast(0)
        val graceUsed = if (isEscalated) 0 else minOf(safePlanned, safeGrace)
        val overBudget = (safePlanned - graceUsed).coerceAtLeast(0)
        val durationPenalty = overBudget * POINTS_PER_OVER_BUDGET_MINUTE
        val baseRelapse = if (isEscalated) relapsePenaltyPoints.coerceAtLeast(0) else 0

        return SessionCostBreakdown(
            plannedMinutes = safePlanned,
            graceMinutesUsed = graceUsed,
            overBudgetMinutes = overBudget,
            baseRelapsePenaltyPoints = baseRelapse,
            durationPenaltyPoints = durationPenalty,
            totalPenaltyPoints = baseRelapse + durationPenalty,
            isEscalated = isEscalated
        )
    }

    /**
     * Rounds elapsed foreground duration to whole minutes (minimum 1m, maximum plannedMinutes)
     * using a +20s threshold so a ~2m stay resolves cleanly to 2m and instant exits still cost 1m.
     */
    fun calculateActualSpentMinutes(activeDurationMs: Long, plannedMinutes: Int): Int {
        val safePlanned = plannedMinutes.coerceAtLeast(1)
        return ((activeDurationMs.coerceAtLeast(0L) + 20_000L) / 60_000L)
            .toInt()
            .coerceIn(1, safePlanned)
    }

    fun calculateRefund(record: ActiveSessionRecord, activeDurationMs: Long): SessionRefundResult {
        val actualSpentMinutes = calculateActualSpentMinutes(activeDurationMs, record.plannedMinutes)
        val actualGraceUsed = minOf(actualSpentMinutes, record.graceMinutesCharged.coerceAtLeast(0))
        val actualOverBudgetMinutes = (actualSpentMinutes - actualGraceUsed).coerceAtLeast(0)
        val actualTotalPenalty = record.baseRelapsePenaltyPoints.coerceAtLeast(0) +
            (actualOverBudgetMinutes * POINTS_PER_OVER_BUDGET_MINUTE)

        return SessionRefundResult(
            actualSpentMinutes = actualSpentMinutes,
            graceMinutesRefunded = (record.graceMinutesCharged - actualGraceUsed).coerceAtLeast(0),
            todayPointsRefunded = (record.todayPointsDeducted - actualTotalPenalty).coerceAtLeast(0),
            lifetimePointsRefunded = (record.lifetimePointsDeducted - actualTotalPenalty).coerceAtLeast(0),
            spendablePointsRefunded = (record.spendablePointsDeducted - actualTotalPenalty).coerceAtLeast(0)
        )
    }
}
