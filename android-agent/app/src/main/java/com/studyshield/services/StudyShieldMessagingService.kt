package com.studyshield.services

import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class StudyShieldMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        getSharedPreferences("ss", Context.MODE_PRIVATE).edit().putString("fcm", token).apply()
    }
    override fun onMessageReceived(msg: RemoteMessage) {
        when (msg.data["type"]) {
            "START_SESSION" -> AppMonitorService.start(this)
            "STOP_SESSION"  -> AppMonitorService.stop(this)
        }
    }
}
