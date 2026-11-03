package com.holdup.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.holdup.app.HoldUpApp
import com.holdup.app.data.model.*
import com.holdup.app.data.preferences.PreferencesManager
import com.holdup.app.ui.InterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HoldUpAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0L

    // In-memory fast cache to make interception zero-latency (no async DataStore disk reads before launch)
    @Volatile private var isMonitoringCache: Boolean = true
    @Volatile private var monitoredPackagesCache: Set<String> = PreferencesManager.DEFAULT_TARGET_PACKAGES
    @Volatile private var isChildSafetyCache: Boolean = false
    @Volatile private var perAppRulesCache: Map<String, AppRuleConfig> = emptyMap()
    @Volatile private var activeSessionsCache: Map<String, Long> = emptyMap()

    companion object {
        var instance: HoldUpAccessibilityService? = null
            private set

        @Volatile
        private var suppressedPackage: String? = null
        @Volatile
        private var suppressUntil: Long = 0L

        fun suppressInterception(packageName: String, durationMs: Long = 1500L) {
            suppressedPackage = packageName
            suppressUntil = System.currentTimeMillis() + durationMs
        }

        fun performGoHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val prefs = HoldUpApp.instance.preferencesManager
        serviceScope.launch { prefs.isMonitoringEnabled.collect { isMonitoringCache = it } }
        serviceScope.launch { prefs.monitoredPackages.collect { monitoredPackagesCache = it } }
        serviceScope.launch { prefs.isChildSafetyEnabled.collect { isChildSafetyCache = it } }
        serviceScope.launch { prefs.perAppRules.collect { perAppRulesCache = it } }
        serviceScope.launch { prefs.activeSessions.collect { activeSessionsCache = it } }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val targetPackageName = event.packageName?.toString() ?: return

        // Skip our own app
        if (targetPackageName == packageName) {
            return
        }

        // Notify session monitor service of foreground package changes
        SessionMonitorService.notifyForegroundPackage(targetPackageName)

        // Skip system launcher or systemui for interception
        if (targetPackageName == "com.android.systemui" ||
            targetPackageName.contains("launcher") ||
            targetPackageName.contains("home")
        ) {
            lastInterceptedPackage = null
            return
        }

        val now = System.currentTimeMillis()

        // Skip temporarily suppressed package (e.g. during Walk Away transition)
        if (targetPackageName == suppressedPackage && now < suppressUntil) {
            return
        }

        // Check if monitoring is enabled and package is in monitored list
        if (!isMonitoringCache || !monitoredPackagesCache.contains(targetPackageName)) {
            return
        }

        // Check if there is an active allowed session
        val sessionExpiry = activeSessionsCache[targetPackageName] ?: 0L
        if (sessionExpiry > now) {
            return
        }

        // Check per-app schedule window if configured
        val rule = perAppRulesCache[targetPackageName]
        if (rule != null && !rule.isCurrentlyActive()) {
            return
        }

        // Check if intervention is currently active for this package (e.g. game loading screen finished
        // and game tried to pop up its main activity over the intervention activity)
        if (InterventionActivity.isInterventionActive && InterventionActivity.activeTargetPackage == targetPackageName) {
            val bringToFrontIntent = Intent(this, InterventionActivity::class.java).apply {
                putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, targetPackageName)
                putExtra(InterventionActivity.EXTRA_IS_SOFT_NUDGE, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(bringToFrontIntent)
            return
        }

        // Debounce rapid duplicate window events for the same package (within 500ms)
        if (targetPackageName == lastInterceptedPackage && (now - lastInterceptTime) < 500) {
            return
        }

        // Hard Lockout: Instant Close for monitored apps (no session proceed or countdown)
        if (isChildSafetyCache) {
            lastInterceptedPackage = targetPackageName
            lastInterceptTime = now
            performGoHome()
            serviceScope.launch {
                val prefs = HoldUpApp.instance.preferencesManager
                prefs.syncCleanFocus()
                prefs.recordInterception(targetPackageName)
            }
            Toast.makeText(this, "🔒 App closed by Hard Lockout", Toast.LENGTH_SHORT).show()
            return
        }

        // Update debounce tracker
        lastInterceptedPackage = targetPackageName
        lastInterceptTime = now

        // Launch Mindful Intervention Activity INSTANTLY without waiting for DataStore I/O
        val intent = Intent(this, InterventionActivity::class.java).apply {
            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, targetPackageName)
            putExtra(InterventionActivity.EXTRA_IS_SOFT_NUDGE, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)

        // Asynchronously update points and interception stats in the background
        serviceScope.launch {
            val prefs = HoldUpApp.instance.preferencesManager
            prefs.syncCleanFocus()
            prefs.recordInterception(targetPackageName)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
