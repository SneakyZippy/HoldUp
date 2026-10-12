package com.holdup.app.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.holdup.app.HoldUpApp
import com.holdup.app.ui.InterventionActivity
import kotlinx.coroutines.*

class SessionMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var timerJob: Job? = null
    private var exitGraceJob: Job? = null

    private var currentMonitoredPackage: String? = null
    private var isAppInForeground: Boolean = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                onScreenTurnedOff()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "holdup_sessions_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SESSION = "com.holdup.action.START_SESSION"
        const val ACTION_EXTEND_SESSION = "com.holdup.action.EXTEND_SESSION"
        const val ACTION_STOP_SESSION = "com.holdup.action.STOP_SESSION"

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        @Volatile
        var activePackageName: String? = null
            private set

        @Volatile
        private var instance: SessionMonitorService? = null

        fun startSession(context: Context, packageName: String, minutes: Int) {
            val intent = Intent(context, SessionMonitorService::class.java).apply {
                action = ACTION_START_SESSION
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_DURATION_MINUTES, minutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, SessionMonitorService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
            context.startService(intent)
        }

        fun notifyForegroundPackage(packageName: String) {
            instance?.onForegroundPackageChanged(packageName)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val targetPackage = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 5) ?: 5

        when (action) {
            ACTION_START_SESSION, ACTION_EXTEND_SESSION -> {
                if (targetPackage.isNotBlank()) {
                    startMonitoring(targetPackage, durationMinutes)
                }
            }
            ACTION_STOP_SESSION -> {
                handleSessionEnded(wasCompleted = false, targetPackage = currentMonitoredPackage ?: targetPackage)
            }
        }

        return START_NOT_STICKY
    }

    private fun startMonitoring(targetPackage: String, minutes: Int) {
        timerJob?.cancel()
        exitGraceJob?.cancel()

        currentMonitoredPackage = targetPackage
        activePackageName = targetPackage
        isAppInForeground = true

        val notification = buildOngoingNotification(targetPackage, minutes)
        startForeground(NOTIFICATION_ID, notification)

        timerJob = serviceScope.launch {
            val totalSeconds = minutes * 60
            for (remainingSec in totalSeconds downTo 1) {
                delay(1000L)
                if (remainingSec % 60 == 0) {
                    val remainingMins = remainingSec / 60
                    val updatedNotification = buildOngoingNotification(targetPackage, remainingMins)
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                }
            }

            // Time expired while in app! Only trigger soft nudge if user remained in app
            val shouldNudge = isAppInForeground && currentMonitoredPackage == targetPackage
            if (shouldNudge) {
                launchSoftNudge(targetPackage)
            }
            handleSessionEnded(wasCompleted = true, targetPackage = targetPackage)
        }
    }

    fun onForegroundPackageChanged(foregroundPackage: String) {
        val monitored = currentMonitoredPackage ?: return

        // Ignore internal or system UI windows that don't represent navigating away
        if (isIgnoredPackage(foregroundPackage)) {
            return
        }

        if (foregroundPackage == monitored) {
            // User is actively in the monitored app
            isAppInForeground = true
            exitGraceJob?.cancel()
            exitGraceJob = null
        } else {
            // User switched away to launcher, home, or another app
            isAppInForeground = false
            if (exitGraceJob?.isActive != true) {
                exitGraceJob = serviceScope.launch {
                    // Grace period of 3 seconds in case of quick app-switch gesture
                    delay(3000L)
                    // If after 3 seconds the app is still not foreground, cancel session and timer
                    handleSessionEnded(wasCompleted = false, targetPackage = monitored)
                }
            }
        }
    }

    private fun onScreenTurnedOff() {
        val monitored = currentMonitoredPackage ?: return
        isAppInForeground = false
        exitGraceJob?.cancel()
        exitGraceJob = serviceScope.launch {
            delay(2000L)
            handleSessionEnded(wasCompleted = false, targetPackage = monitored)
        }
    }

    private fun isIgnoredPackage(pkg: String): Boolean {
        return pkg == packageName ||
                pkg == "com.android.systemui" ||
                pkg == "android" ||
                pkg.contains("inputmethod") ||
                pkg.contains("keyboard") ||
                pkg.contains("ime")
    }

    private fun handleSessionEnded(wasCompleted: Boolean, targetPackage: String) {
        timerJob?.cancel()
        exitGraceJob?.cancel()
        currentMonitoredPackage = null
        activePackageName = null
        isAppInForeground = false

        // Clear active session in preferences so user is guarded next time
        if (targetPackage.isNotBlank()) {
            serviceScope.launch {
                HoldUpApp.instance.preferencesManager.clearSession(targetPackage)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun launchSoftNudge(targetPackage: String) {
        val intent = Intent(this, InterventionActivity::class.java).apply {
            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, targetPackage)
            putExtra(InterventionActivity.EXTRA_IS_SOFT_NUDGE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    private fun buildOngoingNotification(targetPackage: String, remainingMinutes: Int): Notification {
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(targetPackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "App"
        }

        val stopIntent = Intent(this, SessionMonitorService::class.java).apply {
            action = ACTION_STOP_SESSION
            putExtra(EXTRA_PACKAGE_NAME, targetPackage)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HoldUp • Mindful Session Active")
            .setContentText("$appName session: ~$remainingMinutes min left")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Session", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HoldUp Sessions & Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active session timers and mindful gentle reminders"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        if (instance == this) {
            instance = null
        }
        activePackageName = null
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
