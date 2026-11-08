package com.holdup.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.holdup.app.data.model.*
import com.holdup.app.ui.theme.AccentPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore by preferencesDataStore(name = "holdup_preferences")

class PreferencesManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        private val KEY_MONITORED_PACKAGES = stringSetPreferencesKey("monitored_packages")
        private val KEY_GLOBAL_SETTINGS = stringPreferencesKey("global_settings_json")
        private val KEY_PER_APP_RULES = stringPreferencesKey("per_app_rules_json")
        private val KEY_CUSTOM_ACTIVITIES = stringPreferencesKey("custom_activities_json")
        private val KEY_TODAY_DATE = stringPreferencesKey("stats_today_date")
        private val KEY_TOTAL_INTERCEPTIONS = intPreferencesKey("stats_total_interceptions")
        private val KEY_WALKED_AWAY = intPreferencesKey("stats_walked_away")
        private val KEY_PROCEEDED = intPreferencesKey("stats_proceeded")
        private val KEY_MINUTES_SAVED = intPreferencesKey("stats_minutes_saved")
        private val KEY_ACTIVE_SESSIONS = stringPreferencesKey("active_sessions_json")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_CUSTOM_REFLECTIONS = stringSetPreferencesKey("custom_reflections")
        private val KEY_DISMISSED_REFLECTIONS = stringSetPreferencesKey("dismissed_reflections")
        private val KEY_POINTS_TODAY = intPreferencesKey("stats_points_today")
        private val KEY_LIFETIME_POINTS = intPreferencesKey("stats_lifetime_points")
        private val KEY_LAST_INTERVENTION_TYPE = stringPreferencesKey("last_intervention_type")
        private val KEY_SHUFFLE_BAG = stringPreferencesKey("shuffle_bag_json")
        private val KEY_LAST_REFLECTION_QUOTE = stringPreferencesKey("last_reflection_quote")
        private val KEY_LAST_CHOSEN_DURATIONS = stringPreferencesKey("last_chosen_durations_json")
        private val KEY_CHILD_SAFETY_ENABLED = booleanPreferencesKey("child_safety_enabled")
        private val KEY_PARENT_PIN_HASH = stringPreferencesKey("parent_pin_hash")
        private val KEY_PARENT_PIN_SALT = stringPreferencesKey("parent_pin_salt")
        private val KEY_TEMP_SETTINGS_UNLOCK_UNTIL = longPreferencesKey("temp_settings_unlock_until")
        private val KEY_RELAPSE_RESISTANCE_ENABLED = booleanPreferencesKey("relapse_resistance_enabled")
        private val KEY_APP_INTERVENTION_TIMESTAMPS = stringPreferencesKey("app_intervention_timestamps_json")
        private val KEY_LAST_INTERRUPTION_TIMESTAMP = longPreferencesKey("last_interruption_timestamp")
        private val KEY_CLEAN_HOURS_AWARDED_TODAY = intPreferencesKey("clean_hours_awarded_today")
        private val KEY_CLEAN_DAY_BONUS_AWARDED = booleanPreferencesKey("clean_day_bonus_awarded")
        private val KEY_ACCENT_COLOR_PRESET = stringPreferencesKey("accent_color_preset")

        // Popular doomscroll apps pre-populated as defaults
        val DEFAULT_TARGET_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",      // TikTok
            "com.ss.android.ugc.trill",      // TikTok alternative pkg
            "com.twitter.android",           // X / Twitter
            "com.facebook.katana",          // Facebook
            "com.reddit.frontpage",          // Reddit
            "com.google.android.youtube"     // YouTube
        )
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val isMonitoringEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_MONITORING_ENABLED] ?: true
    }

    val monitoredPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_MONITORED_PACKAGES] ?: DEFAULT_TARGET_PACKAGES
    }

    val globalSettings: Flow<GlobalInterventionSettings> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_GLOBAL_SETTINGS]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<GlobalInterventionSettings>(raw)
            } catch (e: Exception) {
                GlobalInterventionSettings()
            }
        } else {
            GlobalInterventionSettings()
        }
    }

    val perAppRules: Flow<Map<String, AppRuleConfig>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_PER_APP_RULES]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<Map<String, AppRuleConfig>>(raw)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    val alternativeActivities: Flow<List<AlternativeActivity>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_ACTIVITIES]
        if (!raw.isNullOrBlank()) {
            try {
                val saved = json.decodeFromString<List<AlternativeActivity>>(raw)
                val savedIds = saved.map { it.id }.toSet()
                val missingDefaults = defaultAlternativeActivities.filter { it.id !in savedIds }
                if (missingDefaults.isNotEmpty()) {
                    saved + missingDefaults
                } else {
                    saved
                }
            } catch (e: Exception) {
                defaultAlternativeActivities
            }
        } else {
            defaultAlternativeActivities
        }
    }

    val isRelapseResistanceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_RELAPSE_RESISTANCE_ENABLED] ?: true
    }

    suspend fun setRelapseResistanceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RELAPSE_RESISTANCE_ENABLED] = enabled
        }
    }

    val accentPreset: Flow<AccentPreset> = context.dataStore.data.map { prefs ->
        AccentPreset.fromKey(prefs[KEY_ACCENT_COLOR_PRESET])
    }

    suspend fun setAccentPreset(preset: AccentPreset) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR_PRESET] = preset.key
        }
    }

    private fun calculateCleanFocusMinutes(lastInterruption: Long, now: Long): Int {
        val calStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startWindow = calStart.timeInMillis
        val endWindow = calEnd.timeInMillis

        if (now < startWindow) return 0

        val streakStart = maxOf(startWindow, lastInterruption)
        val effectiveEnd = minOf(now, endWindow)

        val cleanMs = if (effectiveEnd > streakStart) effectiveEnd - streakStart else 0L
        return (cleanMs / 60000L).toInt()
    }

    val todayStats: Flow<DailyStats> = context.dataStore.data.map { prefs ->
        val savedDate = prefs[KEY_TODAY_DATE] ?: ""
        val today = getTodayDateString()
        val lifetime = prefs[KEY_LIFETIME_POINTS] ?: 0
        val isToday = savedDate == today

        val totalInterceptions = if (isToday) prefs[KEY_TOTAL_INTERCEPTIONS] ?: 0 else 0
        val walkedAway = if (isToday) prefs[KEY_WALKED_AWAY] ?: 0 else 0
        val proceeded = if (isToday) prefs[KEY_PROCEEDED] ?: 0 else 0
        val minutesSaved = if (isToday) prefs[KEY_MINUTES_SAVED] ?: 0 else 0
        val pointsToday = if (isToday) prefs[KEY_POINTS_TODAY] ?: 0 else 0
        val cleanHoursAwarded = if (isToday) prefs[KEY_CLEAN_HOURS_AWARDED_TODAY] ?: 0 else 0
        val isCleanBonusAwarded = if (isToday) prefs[KEY_CLEAN_DAY_BONUS_AWARDED] ?: false else false

        val isMonitoring = prefs[KEY_MONITORING_ENABLED] ?: true
        val monitoredSet = prefs[KEY_MONITORED_PACKAGES] ?: DEFAULT_TARGET_PACKAGES
        val isShieldActive = isMonitoring && monitoredSet.isNotEmpty()

        val cleanMinutes = if (isShieldActive) {
            calculateCleanFocusMinutes(
                lastInterruption = prefs[KEY_LAST_INTERRUPTION_TIMESTAMP] ?: 0L,
                now = System.currentTimeMillis()
            )
        } else {
            0
        }

        DailyStats(
            dateString = today,
            totalInterceptions = totalInterceptions,
            walkedAwayCount = walkedAway,
            proceededCount = proceeded,
            estimatedMinutesSaved = minutesSaved,
            pointsEarnedToday = pointsToday,
            lifetimePoints = lifetime,
            cleanFocusMinutesToday = cleanMinutes,
            cleanFocusHoursAwarded = cleanHoursAwarded,
            isCleanDayBonusAwarded = isCleanBonusAwarded
        )
    }

    val isChildSafetyEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CHILD_SAFETY_ENABLED] ?: false
    }

    val hasParentPin: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_PARENT_PIN_HASH].isNullOrBlank()
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MONITORING_ENABLED] = enabled
            if (enabled) {
                prefs[KEY_LAST_INTERRUPTION_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setMonitoredPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MONITORED_PACKAGES] = packages
        }
    }

    suspend fun toggleAppMonitored(packageName: String, isMonitored: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_MONITORED_PACKAGES] ?: DEFAULT_TARGET_PACKAGES).toMutableSet()
            if (isMonitored) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            prefs[KEY_MONITORED_PACKAGES] = current
        }
    }

    suspend fun saveGlobalSettings(settings: GlobalInterventionSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLOBAL_SETTINGS] = json.encodeToString(settings)
        }
    }

    suspend fun saveAppRule(config: AppRuleConfig) {
        context.dataStore.edit { prefs ->
            val currentRules = try {
                prefs[KEY_PER_APP_RULES]?.let { json.decodeFromString<Map<String, AppRuleConfig>>(it) } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }.toMutableMap()
            currentRules[config.packageName] = config
            prefs[KEY_PER_APP_RULES] = json.encodeToString(currentRules)
        }
    }

    suspend fun checkRelapseState(packageName: String): RelapseState {
        val prefs = context.dataStore.data.first()
        val isEnabled = prefs[KEY_RELAPSE_RESISTANCE_ENABLED] ?: true
        if (!isEnabled || packageName.isBlank()) {
            return RelapseState(tier = 1, recentAttemptsCount = 1, delaySeconds = 0, penaltyPoints = 0)
        }

        val raw = prefs[KEY_APP_INTERVENTION_TIMESTAMPS] ?: return RelapseState(tier = 1, recentAttemptsCount = 1, delaySeconds = 0, penaltyPoints = 0)
        val historyMap = try {
            json.decodeFromString<Map<String, List<Long>>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }

        val now = System.currentTimeMillis()
        val fifteenMinutesAgo = now - (15 * 60 * 1000L)
        // Exclude events recorded in the last 2 seconds to avoid counting the current interception as a prior attempt
        val priorAttempts = historyMap[packageName]?.filter { it in fifteenMinutesAgo until (now - 2000L) } ?: emptyList()
        val count = priorAttempts.size

        return when {
            count == 0 -> RelapseState(tier = 1, recentAttemptsCount = 1, delaySeconds = 0, penaltyPoints = 0)
            count == 1 -> RelapseState(tier = 2, recentAttemptsCount = 2, delaySeconds = 10, penaltyPoints = 5)
            else -> RelapseState(tier = 3, recentAttemptsCount = count + 1, delaySeconds = 20, penaltyPoints = 5)
        }
    }

    suspend fun recordProceedPenalty(points: Int = 5) {
        val today = getTodayDateString()
        context.dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate == today) {
                val currentToday = prefs[KEY_POINTS_TODAY] ?: 0
                prefs[KEY_POINTS_TODAY] = (currentToday - points).coerceAtLeast(0)
            }
            val currentLifetime = prefs[KEY_LIFETIME_POINTS] ?: 0
            prefs[KEY_LIFETIME_POINTS] = (currentLifetime - points).coerceAtLeast(0)
        }
    }

    private fun checkAndAwardPendingBonus(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        val prevCleanBonusAwarded = prefs[KEY_CLEAN_DAY_BONUS_AWARDED] ?: false
        if (!prevCleanBonusAwarded) {
            val prevTotal = prefs[KEY_TOTAL_INTERCEPTIONS] ?: 0
            val prevProceeded = prefs[KEY_PROCEEDED] ?: 0
            val bonus = when {
                prevTotal == 0 -> 50
                prevTotal in 1..2 && prevProceeded == 0 -> 25
                else -> 0
            }
            if (bonus > 0) {
                val lifetime = prefs[KEY_LIFETIME_POINTS] ?: 0
                prefs[KEY_LIFETIME_POINTS] = lifetime + bonus
            }
        }
    }

    suspend fun syncCleanFocus(): DailyStats {
        val today = getTodayDateString()
        val now = System.currentTimeMillis()
        var newPointsToAdd = 0

        context.dataStore.edit { prefs ->
            val isMonitoring = prefs[KEY_MONITORING_ENABLED] ?: true
            val monitoredSet = prefs[KEY_MONITORED_PACKAGES] ?: DEFAULT_TARGET_PACKAGES
            val isShieldActive = isMonitoring && monitoredSet.isNotEmpty()
            if (!isShieldActive) {
                return@edit
            }
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate != today) {
                if (savedDate.isNotBlank()) {
                    checkAndAwardPendingBonus(prefs)
                }
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TOTAL_INTERCEPTIONS] = 0
                prefs[KEY_WALKED_AWAY] = 0
                prefs[KEY_PROCEEDED] = 0
                prefs[KEY_MINUTES_SAVED] = 0
                prefs[KEY_POINTS_TODAY] = 0
                prefs[KEY_CLEAN_HOURS_AWARDED_TODAY] = 0
                prefs[KEY_CLEAN_DAY_BONUS_AWARDED] = false
            }

            val lastInterruption = prefs[KEY_LAST_INTERRUPTION_TIMESTAMP] ?: 0L
            val cleanMinutes = calculateCleanFocusMinutes(lastInterruption, now)
            val fullHours = cleanMinutes / 60
            val alreadyAwardedHours = prefs[KEY_CLEAN_HOURS_AWARDED_TODAY] ?: 0

            if (fullHours > alreadyAwardedHours) {
                val hoursToAward = fullHours - alreadyAwardedHours
                newPointsToAdd += hoursToAward * 10
                prefs[KEY_CLEAN_HOURS_AWARDED_TODAY] = fullHours
            }

            val calEnd = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 22)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val isPast22 = now >= calEnd.timeInMillis
            val cleanBonusAwarded = prefs[KEY_CLEAN_DAY_BONUS_AWARDED] ?: false

            if (isPast22 && !cleanBonusAwarded) {
                val total = prefs[KEY_TOTAL_INTERCEPTIONS] ?: 0
                val proceeded = prefs[KEY_PROCEEDED] ?: 0
                if (total == 0) {
                    newPointsToAdd += 50
                    prefs[KEY_CLEAN_DAY_BONUS_AWARDED] = true
                } else if (total in 1..2 && proceeded == 0) {
                    newPointsToAdd += 25
                    prefs[KEY_CLEAN_DAY_BONUS_AWARDED] = true
                }
            }

            if (newPointsToAdd > 0) {
                val currentToday = prefs[KEY_POINTS_TODAY] ?: 0
                val currentLifetime = prefs[KEY_LIFETIME_POINTS] ?: 0
                prefs[KEY_POINTS_TODAY] = currentToday + newPointsToAdd
                prefs[KEY_LIFETIME_POINTS] = currentLifetime + newPointsToAdd
            }
        }
        return todayStats.first()
    }

    suspend fun recordInterception(packageName: String) {
        val today = getTodayDateString()
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate != today) {
                if (savedDate.isNotBlank()) {
                    checkAndAwardPendingBonus(prefs)
                }
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TOTAL_INTERCEPTIONS] = 1
                prefs[KEY_WALKED_AWAY] = 0
                prefs[KEY_PROCEEDED] = 0
                prefs[KEY_MINUTES_SAVED] = 0
                prefs[KEY_POINTS_TODAY] = 0
                prefs[KEY_CLEAN_HOURS_AWARDED_TODAY] = 0
                prefs[KEY_CLEAN_DAY_BONUS_AWARDED] = false
            } else {
                val current = prefs[KEY_TOTAL_INTERCEPTIONS] ?: 0
                prefs[KEY_TOTAL_INTERCEPTIONS] = current + 1
            }


            // Update app intervention history for Relapse Resistance
            if (packageName.isNotBlank()) {
                val raw = prefs[KEY_APP_INTERVENTION_TIMESTAMPS]
                val currentMap = try {
                    if (!raw.isNullOrBlank()) json.decodeFromString<Map<String, List<Long>>>(raw) else emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }.toMutableMap()

                val oneDayAgo = now - (24 * 60 * 60 * 1000L)
                val list = (currentMap[packageName] ?: emptyList()).filter { it >= oneDayAgo }.toMutableList()
                list.add(now)
                currentMap[packageName] = list
                prefs[KEY_APP_INTERVENTION_TIMESTAMPS] = json.encodeToString(currentMap)
            }
        }
    }

    suspend fun recordWalkAway(minutesSavedEstimate: Int = 15, isAlternativeActivity: Boolean = false) {
        val today = getTodayDateString()
        context.dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate == today) {
                val walked = prefs[KEY_WALKED_AWAY] ?: 0
                val saved = prefs[KEY_MINUTES_SAVED] ?: 0
                prefs[KEY_WALKED_AWAY] = walked + 1
                prefs[KEY_MINUTES_SAVED] = saved + minutesSavedEstimate
            } else {
                if (savedDate.isNotBlank()) {
                    checkAndAwardPendingBonus(prefs)
                }
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TOTAL_INTERCEPTIONS] = 1
                prefs[KEY_WALKED_AWAY] = 1
                prefs[KEY_PROCEEDED] = 0
                prefs[KEY_MINUTES_SAVED] = minutesSavedEstimate
            }
        }
    }

    suspend fun recordProceedSession(packageName: String, durationMinutes: Int) {
        val today = getTodayDateString()
        val now = System.currentTimeMillis()
        val expiresAt = now + (durationMinutes * 60 * 1000L)
        context.dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate == today) {
                val proceeded = prefs[KEY_PROCEEDED] ?: 0
                prefs[KEY_PROCEEDED] = proceeded + 1
            } else {
                if (savedDate.isNotBlank()) {
                    checkAndAwardPendingBonus(prefs)
                }
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TOTAL_INTERCEPTIONS] = 1
                prefs[KEY_WALKED_AWAY] = 0
                prefs[KEY_PROCEEDED] = 1
                prefs[KEY_MINUTES_SAVED] = 0
            }

            prefs[KEY_LAST_INTERRUPTION_TIMESTAMP] = now

            // Update active sessions map
            val activeSessions = try {
                prefs[KEY_ACTIVE_SESSIONS]?.let { json.decodeFromString<Map<String, Long>>(it) } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }.toMutableMap()
            activeSessions[packageName] = expiresAt
            prefs[KEY_ACTIVE_SESSIONS] = json.encodeToString(activeSessions)
        }
    }

    val activeSessions: Flow<Map<String, Long>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_ACTIVE_SESSIONS]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<Map<String, Long>>(raw)
            } catch (_: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    suspend fun isSessionActive(packageName: String): Boolean {
        val prefs = context.dataStore.data.first()
        val raw = prefs[KEY_ACTIVE_SESSIONS] ?: return false
        return try {
            val sessions = json.decodeFromString<Map<String, Long>>(raw)
            val expiresAt = sessions[packageName] ?: return false
            expiresAt > System.currentTimeMillis()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearSession(packageName: String) {
        context.dataStore.edit { prefs ->
            val activeSessions = try {
                prefs[KEY_ACTIVE_SESSIONS]?.let { json.decodeFromString<Map<String, Long>>(it) } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }.toMutableMap()
            activeSessions.remove(packageName)
            prefs[KEY_ACTIVE_SESSIONS] = json.encodeToString(activeSessions)
        }
    }

    suspend fun saveLastChosenDuration(packageName: String, minutes: Int) {
        context.dataStore.edit { prefs ->
            val current = try {
                prefs[KEY_LAST_CHOSEN_DURATIONS]?.let { json.decodeFromString<Map<String, Int>>(it) } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }.toMutableMap()
            current[packageName] = minutes
            prefs[KEY_LAST_CHOSEN_DURATIONS] = json.encodeToString(current)
        }
    }

    suspend fun getLastChosenDuration(packageName: String): Int? {
        val prefs = context.dataStore.data.first()
        val raw = prefs[KEY_LAST_CHOSEN_DURATIONS] ?: return null
        return try {
            val map = json.decodeFromString<Map<String, Int>>(raw)
            map[packageName]
        } catch (e: Exception) {
            null
        }
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    val customReflections: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_REFLECTIONS]?.toList() ?: emptyList()
    }

    val dismissedReflections: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_DISMISSED_REFLECTIONS] ?: emptySet()
    }

    suspend fun addCustomReflection(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_REFLECTIONS] ?: emptySet()
            prefs[KEY_CUSTOM_REFLECTIONS] = current + clean
        }
    }

    suspend fun removeCustomReflection(text: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_REFLECTIONS] ?: emptySet()
            prefs[KEY_CUSTOM_REFLECTIONS] = current - text
        }
    }

    suspend fun dismissReflection(text: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DISMISSED_REFLECTIONS] ?: emptySet()
            prefs[KEY_DISMISSED_REFLECTIONS] = current + text
        }
    }

    suspend fun restoreReflection(text: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DISMISSED_REFLECTIONS] ?: emptySet()
            prefs[KEY_DISMISSED_REFLECTIONS] = current - text
        }
    }

    suspend fun resetDismissedReflections() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_DISMISSED_REFLECTIONS)
        }
    }

    suspend fun saveAlternativeActivities(activities: List<AlternativeActivity>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_ACTIVITIES] = json.encodeToString(activities)
        }
    }

    suspend fun toggleAlternativeActivity(id: String) {
        val current = alternativeActivities.first()
        val updated = current.map { activity ->
            if (activity.id == id) activity.copy(isEnabled = !activity.isEnabled) else activity
        }
        saveAlternativeActivities(updated)
    }

    suspend fun addAlternativeActivity(title: String, emoji: String, category: String = "Custom") {
        val current = alternativeActivities.first()
        val newActivity = AlternativeActivity(
            id = "custom_${System.currentTimeMillis()}",
            title = title.trim(),
            iconEmoji = emoji.ifBlank { "✨" },
            category = category,
            isEnabled = true,
            isCustom = true
        )
        saveAlternativeActivities(listOf(newActivity) + current)
    }

    suspend fun updateAlternativeActivity(id: String, newTitle: String, newEmoji: String) {
        val current = alternativeActivities.first()
        val updated = current.map { activity ->
            if (activity.id == id) activity.copy(title = newTitle.trim(), iconEmoji = newEmoji.ifBlank { activity.iconEmoji }) else activity
        }
        saveAlternativeActivities(updated)
    }

    suspend fun deleteAlternativeActivity(id: String) {
        val current = alternativeActivities.first()
        val updated = current.filter { it.id != id }
        saveAlternativeActivities(updated)
    }

    suspend fun setAllAlternativeActivitiesEnabled(enabled: Boolean) {
        val current = alternativeActivities.first()
        val updated = current.map { it.copy(isEnabled = enabled) }
        saveAlternativeActivities(updated)
    }

    suspend fun getNextInterventionType(
        mode: RuleMode,
        eligibleTypes: List<InterventionType>
    ): InterventionType {
        if (eligibleTypes.isEmpty()) return InterventionType.BREATHING
        if (eligibleTypes.size == 1) return eligibleTypes.first()

        val prefs = context.dataStore.data.first()
        val lastTypeName = prefs[KEY_LAST_INTERVENTION_TYPE]

        val selectedType: InterventionType = when (mode) {
            RuleMode.ROTATE, RuleMode.SEQUENCE -> {
                val lastIndex = eligibleTypes.indexOfFirst { it.name == lastTypeName }
                val nextIndex = if (lastIndex != -1) {
                    (lastIndex + 1) % eligibleTypes.size
                } else {
                    0
                }
                eligibleTypes[nextIndex]
            }
            RuleMode.SHUFFLE -> {
                val rawBag = prefs[KEY_SHUFFLE_BAG]
                var currentBag: List<String> = try {
                    if (!rawBag.isNullOrBlank()) json.decodeFromString<List<String>>(rawBag) else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                // Filter bag to only currently eligible types
                currentBag = currentBag.filter { name -> eligibleTypes.any { it.name == name } }

                // If bag is empty, create a fresh shuffled bag
                if (currentBag.isEmpty()) {
                    val shuffled = eligibleTypes.map { it.name }.shuffled().toMutableList()
                    // Prevent immediate repetition from previous cycle if more than 1 item
                    if (shuffled.size > 1 && shuffled.first() == lastTypeName) {
                        val temp = shuffled[0]
                        shuffled[0] = shuffled[1]
                        shuffled[1] = temp
                    }
                    currentBag = shuffled
                }

                val chosenName = currentBag.first()
                val remainingBag = currentBag.drop(1)
                context.dataStore.edit { editPrefs ->
                    editPrefs[KEY_SHUFFLE_BAG] = json.encodeToString(remainingBag)
                }

                eligibleTypes.firstOrNull { it.name == chosenName } ?: eligibleTypes.first()
            }
            RuleMode.RANDOM -> {
                eligibleTypes.random()
            }
            RuleMode.SPECIFIC -> {
                eligibleTypes.first()
            }
        }

        context.dataStore.edit { editPrefs ->
            editPrefs[KEY_LAST_INTERVENTION_TYPE] = selectedType.name
        }

        return selectedType
    }

    suspend fun getNextReflectionQuote(availableQuotes: List<String>): String {
        if (availableQuotes.isEmpty()) return "Take a slow, deep breath."
        if (availableQuotes.size == 1) return availableQuotes.first()

        val prefs = context.dataStore.data.first()
        val lastQuote = prefs[KEY_LAST_REFLECTION_QUOTE]

        val candidates = availableQuotes.filter { it != lastQuote }
        val chosen = if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            availableQuotes.random()
        }

        context.dataStore.edit { editPrefs ->
            editPrefs[KEY_LAST_REFLECTION_QUOTE] = chosen
        }

        return chosen
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun setChildSafetyEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHILD_SAFETY_ENABLED] = enabled
        }
    }

    suspend fun setParentPin(pin: String) {
        val random = java.security.SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        val salt = saltBytes.joinToString("") { "%02x".format(it) }
        val hash = hashPin(pin, salt)

        context.dataStore.edit { prefs ->
            prefs[KEY_PARENT_PIN_SALT] = salt
            prefs[KEY_PARENT_PIN_HASH] = hash
        }
    }

    suspend fun verifyParentPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[KEY_PARENT_PIN_SALT] ?: return false
        val savedHash = prefs[KEY_PARENT_PIN_HASH] ?: return false
        val computed = hashPin(pin, salt)
        return computed.equals(savedHash, ignoreCase = true)
    }
}
