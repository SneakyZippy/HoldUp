package com.holdup.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.holdup.app.data.model.*
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
                json.decodeFromString<List<AlternativeActivity>>(raw)
            } catch (e: Exception) {
                defaultAlternativeActivities
            }
        } else {
            defaultAlternativeActivities
        }
    }

    val todayStats: Flow<DailyStats> = context.dataStore.data.map { prefs ->
        val savedDate = prefs[KEY_TODAY_DATE] ?: ""
        val today = getTodayDateString()
        val lifetime = prefs[KEY_LIFETIME_POINTS] ?: 0
        if (savedDate == today) {
            DailyStats(
                dateString = today,
                totalInterceptions = prefs[KEY_TOTAL_INTERCEPTIONS] ?: 0,
                walkedAwayCount = prefs[KEY_WALKED_AWAY] ?: 0,
                proceededCount = prefs[KEY_PROCEEDED] ?: 0,
                estimatedMinutesSaved = prefs[KEY_MINUTES_SAVED] ?: 0,
                pointsEarnedToday = prefs[KEY_POINTS_TODAY] ?: 0,
                lifetimePoints = lifetime
            )
        } else {
            DailyStats(
                dateString = today,
                lifetimePoints = lifetime
            )
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MONITORING_ENABLED] = enabled
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

    suspend fun recordInterception(packageName: String) {
        val today = getTodayDateString()
        context.dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate != today) {
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TOTAL_INTERCEPTIONS] = 1
                prefs[KEY_WALKED_AWAY] = 0
                prefs[KEY_PROCEEDED] = 0
                prefs[KEY_MINUTES_SAVED] = 0
            } else {
                val current = prefs[KEY_TOTAL_INTERCEPTIONS] ?: 0
                prefs[KEY_TOTAL_INTERCEPTIONS] = current + 1
            }
        }
    }

    suspend fun recordWalkAway(minutesSavedEstimate: Int = 15, isAlternativeActivity: Boolean = false) {
        val today = getTodayDateString()
        val pointsToAdd = if (isAlternativeActivity) 20 else 10
        context.dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            val lifetime = prefs[KEY_LIFETIME_POINTS] ?: 0
            prefs[KEY_LIFETIME_POINTS] = lifetime + pointsToAdd

            if (savedDate == today) {
                val walked = prefs[KEY_WALKED_AWAY] ?: 0
                val saved = prefs[KEY_MINUTES_SAVED] ?: 0
                val pointsToday = prefs[KEY_POINTS_TODAY] ?: 0
                prefs[KEY_WALKED_AWAY] = walked + 1
                prefs[KEY_MINUTES_SAVED] = saved + minutesSavedEstimate
                prefs[KEY_POINTS_TODAY] = pointsToday + pointsToAdd
            } else {
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TOTAL_INTERCEPTIONS] = 1
                prefs[KEY_WALKED_AWAY] = 1
                prefs[KEY_PROCEEDED] = 0
                prefs[KEY_MINUTES_SAVED] = minutesSavedEstimate
                prefs[KEY_POINTS_TODAY] = pointsToAdd
            }
        }
    }

    suspend fun recordProceedSession(packageName: String, durationMinutes: Int) {
        val today = getTodayDateString()
        val expiresAt = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        context.dataStore.edit { prefs ->
            val savedDate = prefs[KEY_TODAY_DATE] ?: ""
            if (savedDate == today) {
                val proceeded = prefs[KEY_PROCEEDED] ?: 0
                prefs[KEY_PROCEEDED] = proceeded + 1
            } else {
                prefs[KEY_TODAY_DATE] = today
                prefs[KEY_TOTAL_INTERCEPTIONS] = 1
                prefs[KEY_WALKED_AWAY] = 0
                prefs[KEY_PROCEEDED] = 1
                prefs[KEY_MINUTES_SAVED] = 0
            }

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
}
