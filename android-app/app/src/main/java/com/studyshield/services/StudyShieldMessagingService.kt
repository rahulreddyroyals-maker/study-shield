// StudyShieldMessagingService.kt
package com.studyshield.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.studyshield.R
import com.studyshield.ui.MainActivity

class StudyShieldMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FCMService"
        const val CHANNEL_ALERTS = "study_shield_alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
        // Save token to Firestore for the linked child document
        saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: ${message.data}")

        val data = message.data
        val type = data["type"] ?: "notification"

        when (type) {
            "START_SESSION" -> {
                // Parent started session remotely — start monitoring
                AppMonitorService.start(this)
            }
            "STOP_SESSION" -> {
                // Session ended
                AppMonitorService.stop(this)
            }
            "EMERGENCY" -> {
                showEmergencyNotification()
            }
            else -> {
                // Generic notification
                val title = message.notification?.title ?: data["title"] ?: "Study Shield"
                val body = message.notification?.body ?: data["body"] ?: ""
                showNotification(title, body)
            }
        }
    }

    private fun saveFcmToken(token: String) {
        // Done via DeviceIdUtils on startup, but handle refresh here
        val prefs = getSharedPreferences("study_shield", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    private fun showNotification(title: String, body: String) {
        createChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        getSystemService(NotificationManager::class.java).notify(2000, notif)
    }

    private fun showEmergencyNotification() {
        createChannel()
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setContentTitle("⚠️ Emergency alert")
            .setContentText("Your child has triggered the emergency override")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(2001, notif)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ALERTS, "Study Shield Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
