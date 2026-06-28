package com.studyshield.services

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class StudyShieldMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "SSFCM"
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM token refreshed")
        // Save locally — AppMonitorService will pick it up on next Firestore sync
        getSharedPreferences("study_shield_prefs", Context.MODE_PRIVATE)
            .edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i(TAG, "FCM message: ${message.data}")
        when (message.data["type"]) {
            "START_SESSION" -> AppMonitorService.start(this)
            "STOP_SESSION"  -> AppMonitorService.stop(this)
            else -> Log.d(TAG, "Unknown message type: ${message.data["type"]}")
        }
    }
}
