package com.holdup.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.holdup.app.HoldUpApp
import com.holdup.app.data.model.*
import com.holdup.app.ui.InterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HoldUpAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0L

    companion object {
        var instance: HoldUpAccessibilityService? = null
            private set

        @Volatile
        private var suppressedPackage: String? = null
        @Volatile
        private var suppressUntil: Long = 0L

        fun suppressInterception(packageName: String, durationMs: Long = 3500L) {
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

        // Skip temporarily suppressed package (e.g. during Walk Away transition or newly approved session)
        if (targetPackageName == suppressedPackage && now < suppressUntil) {
            return
        }

        // Debounce rapid window changes for the same package (within 2 seconds)
        if (targetPackageName == lastInterceptedPackage && (now - lastInterceptTime) < 2000) {
            return
        }

        serviceScope.launch {
            val prefs = HoldUpApp.instance.preferencesManager
            val isMonitoring = prefs.isMonitoringEnabled.first()
            if (!isMonitoring) return@launch

            val isChildSafety = prefs.isChildSafetyEnabled.first()

            val monitoredList = prefs.monitoredPackages.first()
            if (!monitoredList.contains(targetPackageName)) return@launch

            // Hard Lockout: Instant Close for monitored apps (no session proceed or countdown)
            if (isChildSafety) {
                lastInterceptedPackage = targetPackageName
                lastInterceptTime = now
                performGoHome()
                prefs.recordInterception(targetPackageName)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HoldUpAccessibilityService,
                        "🔒 App closed by Hard Lockout",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            // Check if there is an active allowed session
            val hasActiveSession = prefs.isSessionActive(targetPackageName)
            if (hasActiveSession) return@launch

            // Check per-app schedule window if configured
            val rules = prefs.perAppRules.first()
            val rule = rules[targetPackageName]
            if (rule != null && !rule.isCurrentlyActive()) {
                return@launch
            }

            // Update debounce tracker
            lastInterceptedPackage = targetPackageName
            lastInterceptTime = now

            // Record interception attempt
            prefs.recordInterception(targetPackageName)

            // Launch Mindful Intervention Activity
            val intent = Intent(this@HoldUpAccessibilityService, InterventionActivity::class.java).apply {
                putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, targetPackageName)
                putExtra(InterventionActivity.EXTRA_IS_SOFT_NUDGE, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
