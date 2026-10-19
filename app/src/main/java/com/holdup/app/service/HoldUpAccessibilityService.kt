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

        fun performGoHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller"
        )
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

        // Debounce rapid window changes for the same package (within 1.5 seconds)
        val now = System.currentTimeMillis()
        if (targetPackageName == lastInterceptedPackage && (now - lastInterceptTime) < 1500) {
            return
        }

        serviceScope.launch {
            val prefs = HoldUpApp.instance.preferencesManager
            val isMonitoring = prefs.isMonitoringEnabled.first()
            if (!isMonitoring) return@launch

            val isChildSafety = prefs.isChildSafetyEnabled.first()

            // 1. Guard against tampering via system Settings or Package Installers
            if (isChildSafety && SETTINGS_PACKAGES.contains(targetPackageName)) {
                val isUnlocked = prefs.isSettingsTemporarilyUnlocked()
                if (!isUnlocked) {
                    lastInterceptedPackage = targetPackageName
                    lastInterceptTime = now
                    performGoHome()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@HoldUpAccessibilityService,
                            "🔒 Settings locked by Child Safety. Open HoldUp to unlock.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
            }

            val monitoredList = prefs.monitoredPackages.first()
            if (!monitoredList.contains(targetPackageName)) return@launch

            // 2. Child Safety Instant Close (hard block without countdown or session proceed)
            if (isChildSafety) {
                lastInterceptedPackage = targetPackageName
                lastInterceptTime = now
                performGoHome()
                prefs.recordInterception(targetPackageName)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HoldUpAccessibilityService,
                        "🔒 App locked by Child Safety",
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
