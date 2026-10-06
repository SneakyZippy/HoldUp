package com.holdup.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.holdup.app.HoldUpApp
import com.holdup.app.ui.InterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

        // Skip our own app or system launcher/systemui
        if (targetPackageName == packageName ||
            targetPackageName == "com.android.systemui" ||
            targetPackageName.contains("launcher")
        ) {
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

            val monitoredList = prefs.monitoredPackages.first()
            if (!monitoredList.contains(targetPackageName)) return@launch

            // Check if there is an active allowed session
            val hasActiveSession = prefs.isSessionActive(targetPackageName)
            if (hasActiveSession) return@launch

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
