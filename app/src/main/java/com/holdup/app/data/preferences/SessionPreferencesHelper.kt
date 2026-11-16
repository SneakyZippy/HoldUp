package com.holdup.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.holdup.app.data.model.ActiveSessionRecord
import com.holdup.app.data.model.SessionCostBreakdown
import com.holdup.app.data.model.SessionPenaltyCalculator
import com.holdup.app.data.model.SessionRefundResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Handles atomic DataStore accounting for proceeding into shielded apps and refunding
 * unused Daily Grace minutes or duration-based penalty points on early exit.
 */
internal class SessionPreferencesHelper(
    private val context: Context,
    private val json: Json,
    private val petRepository: PetPreferencesRepository,
    private val getTodayDateString: () -> String,
    private val readDailyGraceBudget: (Preferences) -> Int,
    private val rolloverDayIfNeeded: (MutablePreferences, String) -> Unit
) {

    suspend fun recordProceedSession(
        packageName: String,
        durationMinutes: Int,
        relapsePenaltyPoints: Int = 0
    ): SessionCostBreakdown {
        val today = getTodayDateString()
        val now = System.currentTimeMillis()
        val safeMinutes = durationMinutes.coerceAtLeast(1)
        val expiresAt = now + (safeMinutes * 60_000L)
        var computedCost = SessionCostBreakdown(
            plannedMinutes = safeMinutes,
            graceMinutesUsed = 0,
            overBudgetMinutes = 0,
            baseRelapsePenaltyPoints = 0,
            durationPenaltyPoints = 0,
            totalPenaltyPoints = 0,
            isEscalated = relapsePenaltyPoints > 0
        )

        context.dataStore.edit { prefs ->
            rolloverDayIfNeeded(prefs, today)
            val proceeded = prefs[PreferencesManager.KEY_PROCEEDED] ?: 0
            prefs[PreferencesManager.KEY_PROCEEDED] = proceeded + 1

            val graceBudget = readDailyGraceBudget(prefs)
            val graceUsed = prefs[PreferencesManager.KEY_GRACE_USED_TODAY] ?: 0
            val graceRemaining = (graceBudget - graceUsed).coerceAtLeast(0)

            val cost = SessionPenaltyCalculator.calculateCost(
                plannedMinutes = safeMinutes,
                graceMinutesRemaining = graceRemaining,
                isEscalated = relapsePenaltyPoints > 0,
                relapsePenaltyPoints = relapsePenaltyPoints
            )
            computedCost = cost

            if (cost.graceMinutesUsed > 0) {
                prefs[PreferencesManager.KEY_GRACE_USED_TODAY] = graceUsed + cost.graceMinutesUsed
            }
            if (cost.exceedsGrace) {
                val overBudget = prefs[PreferencesManager.KEY_OVER_BUDGET_OPENS_TODAY] ?: 0
                prefs[PreferencesManager.KEY_OVER_BUDGET_OPENS_TODAY] = overBudget + 1
                prefs[PreferencesManager.KEY_LAST_INTERRUPTION_TIMESTAMP] = now
                petRepository.markActivePetSad(prefs)
            }

            val deductions = deductPenaltyPoints(prefs, cost.totalPenaltyPoints)
            saveSessionStart(prefs, packageName, today, cost, deductions, now, expiresAt)
        }
        return computedCost
    }

    suspend fun clearSession(
        packageName: String,
        wasCompleted: Boolean = true,
        activeDurationMs: Long? = null
    ): SessionRefundResult? {
        val today = getTodayDateString()
        var refundResult: SessionRefundResult? = null

        context.dataStore.edit { prefs ->
            val activeSessions = readActiveSessions(prefs).toMutableMap()
            activeSessions.remove(packageName)
            prefs[PreferencesManager.KEY_ACTIVE_SESSIONS] = json.encodeToString(activeSessions)

            val records = readActiveSessionRecords(prefs).toMutableMap()
            val record = records.remove(packageName)
            prefs[PreferencesManager.KEY_ACTIVE_SESSION_RECORDS] = json.encodeToString(records)

            if (!wasCompleted && activeDurationMs != null && record != null) {
                val refund = SessionPenaltyCalculator.calculateRefund(record, activeDurationMs)
                if (refund.hasRefund) {
                    applyRefund(prefs, record.dateString, today, refund)
                    refundResult = refund
                }
            }
        }
        return refundResult
    }

    private fun deductPenaltyPoints(
        prefs: MutablePreferences,
        totalPenalty: Int
    ): Triple<Int, Int, Int> {
        if (totalPenalty <= 0) return Triple(0, 0, 0)

        val currentToday = prefs[PreferencesManager.KEY_POINTS_TODAY] ?: 0
        val nextToday = (currentToday - totalPenalty).coerceAtLeast(0)
        prefs[PreferencesManager.KEY_POINTS_TODAY] = nextToday

        val currentLifetime = prefs[PreferencesManager.KEY_LIFETIME_POINTS] ?: 0
        val nextLifetime = (currentLifetime - totalPenalty).coerceAtLeast(0)
        prefs[PreferencesManager.KEY_LIFETIME_POINTS] = nextLifetime

        val currentSpendable = petRepository.readSpendablePoints(prefs)
        val nextSpendable = (currentSpendable - totalPenalty).coerceAtLeast(0)
        prefs[PreferencesManager.KEY_SPENDABLE_POINTS] = nextSpendable

        return Triple(
            currentToday - nextToday,
            currentLifetime - nextLifetime,
            currentSpendable - nextSpendable
        )
    }

    private fun saveSessionStart(
        prefs: MutablePreferences,
        packageName: String,
        today: String,
        cost: SessionCostBreakdown,
        deductions: Triple<Int, Int, Int>,
        startedAtMillis: Long,
        expiresAtMillis: Long
    ) {
        val activeSessions = readActiveSessions(prefs).toMutableMap()
        activeSessions[packageName] = expiresAtMillis
        prefs[PreferencesManager.KEY_ACTIVE_SESSIONS] = json.encodeToString(activeSessions)

        val records = readActiveSessionRecords(prefs).toMutableMap()
        records[packageName] = ActiveSessionRecord(
            packageName = packageName,
            dateString = today,
            plannedMinutes = cost.plannedMinutes,
            graceMinutesCharged = cost.graceMinutesUsed,
            baseRelapsePenaltyPoints = cost.baseRelapsePenaltyPoints,
            todayPointsDeducted = deductions.first,
            lifetimePointsDeducted = deductions.second,
            spendablePointsDeducted = deductions.third,
            startedAtMillis = startedAtMillis,
            expiresAtMillis = expiresAtMillis
        )
        prefs[PreferencesManager.KEY_ACTIVE_SESSION_RECORDS] = json.encodeToString(records)
    }

    private fun applyRefund(
        prefs: MutablePreferences,
        recordDate: String,
        today: String,
        refund: SessionRefundResult
    ) {
        val savedDate = prefs[PreferencesManager.KEY_TODAY_DATE] ?: ""
        val isSameDay = savedDate == today && recordDate == today

        if (isSameDay && refund.graceMinutesRefunded > 0) {
            val graceUsed = prefs[PreferencesManager.KEY_GRACE_USED_TODAY] ?: 0
            prefs[PreferencesManager.KEY_GRACE_USED_TODAY] =
                (graceUsed - refund.graceMinutesRefunded).coerceAtLeast(0)
        }
        if (isSameDay && refund.todayPointsRefunded > 0) {
            val pointsToday = prefs[PreferencesManager.KEY_POINTS_TODAY] ?: 0
            prefs[PreferencesManager.KEY_POINTS_TODAY] = pointsToday + refund.todayPointsRefunded
        }
        if (refund.lifetimePointsRefunded > 0) {
            val lifetime = prefs[PreferencesManager.KEY_LIFETIME_POINTS] ?: 0
            prefs[PreferencesManager.KEY_LIFETIME_POINTS] = lifetime + refund.lifetimePointsRefunded
        }
        if (refund.spendablePointsRefunded > 0) {
            val spendable = petRepository.readSpendablePoints(prefs)
            prefs[PreferencesManager.KEY_SPENDABLE_POINTS] = spendable + refund.spendablePointsRefunded
        }
    }

    private fun readActiveSessions(prefs: Preferences): Map<String, Long> {
        val raw = prefs[PreferencesManager.KEY_ACTIVE_SESSIONS] ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, Long>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun readActiveSessionRecords(prefs: Preferences): Map<String, ActiveSessionRecord> {
        val raw = prefs[PreferencesManager.KEY_ACTIVE_SESSION_RECORDS] ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, ActiveSessionRecord>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
