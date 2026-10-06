package com.holdup.app.service

import android.app.*
import android.content.Context
import android.content.Intent
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

    companion object {
        const val CHANNEL_ID = "holdup_sessions_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SESSION = "com.holdup.action.START_SESSION"
        const val ACTION_EXTEND_SESSION = "com.holdup.action.EXTEND_SESSION"
        const val ACTION_STOP_SESSION = "com.holdup.action.STOP_SESSION"

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

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
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val targetPackage = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 5) ?: 5

        when (action) {
            ACTION_START_SESSION, ACTION_EXTEND_SESSION -> {
                startMonitoring(targetPackage, durationMinutes)
            }
            ACTION_STOP_SESSION -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startMonitoring(targetPackage: String, minutes: Int) {
        timerJob?.cancel()

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

            // Time expired! Clear session
            HoldUpApp.instance.preferencesManager.clearSession(targetPackage)

            // Trigger Soft Nudge
            launchSoftNudge(targetPackage)
            stopSelf()
        }
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HoldUp • Mindful Session Active")
            .setContentText("$appName has ~$remainingMinutes min remaining")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
