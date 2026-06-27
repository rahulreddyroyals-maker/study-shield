// AppMonitorService.kt
// ─────────────────────────────────────────────────────────────────────────────
// This is the most critical component. It runs as a foreground service,
// polls UsageStatsManager every 500ms to detect the foreground app, and
// shows a full-screen blocking overlay if a restricted app is launched.
// It also syncs session state in real-time with Firestore.
// ─────────────────────────────────────────────────────────────────────────────
package com.studyshield.services

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.studyshield.R
import com.studyshield.ui.BlockedActivity
import com.studyshield.utils.AppBlockerUtils
import com.studyshield.utils.DeviceIdUtils
import kotlinx.coroutines.*

class AppMonitorService : LifecycleService() {

    companion object {
        const val TAG = "AppMonitor"
        const val CHANNEL_ID = "study_shield_monitor"
        const val NOTIF_ID = 1001
        const val POLL_INTERVAL_MS = 500L

        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitorService::class.java))
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var usageStatsManager: UsageStatsManager
    private var monitorJob: Job? = null
    private var sessionListener: ListenerRegistration? = null

    // Current session state (updated from Firestore in real-time)
    private var sessionActive = false
    private var sessionId: String? = null
    private var childId: String? = null
    private var allowedPackages: Set<String> = emptySet()
    private var sessionEndTimeMs: Long = 0L
    private var lastBlockedApp: String? = null

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        isRunning = true
        Log.d(TAG, "AppMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(
            NOTIF_ID,
            buildNotification("Study Shield active — monitoring apps"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        val deviceId = DeviceIdUtils.getDeviceId(this)
        listenToChildSession(deviceId)
        startMonitorLoop()
        return START_STICKY
    }

    // ── Listen to Firestore for session state changes ─────────────────────────
    private fun listenToChildSession(deviceId: String) {
        // Find child document by deviceId
        db.collection("children")
            .whereEqualTo("deviceId", deviceId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore error: ${error.message}")
                    return@addSnapshotListener
                }
                val childDoc = snapshot?.documents?.firstOrNull() ?: return@addSnapshotListener
                childId = childDoc.id
                val status = childDoc.getString("sessionStatus") ?: "idle"
                val activeSessionId = childDoc.getString("activeSessionId")

                if (status == "active" && activeSessionId != null) {
                    sessionId = activeSessionId
                    listenToSession(activeSessionId, childDoc)
                } else {
                    // Session ended — clear blocking
                    sessionActive = false
                    sessionId = null
                    allowedPackages = emptySet()
                    updateNotification("Study Shield — standby")
                    Log.d(TAG, "Session ended, monitoring stopped")
                }
            }
    }

    private fun listenToSession(sessionId: String, childDoc: DocumentSnapshot) {
        sessionListener?.remove()

        // Get allowed packages from child doc
        @Suppress("UNCHECKED_CAST")
        val allowedApps = childDoc.get("allowedApps") as? List<Map<String, String>> ?: emptyList()
        val allowed = mutableSetOf<String>()
        allowed.add(packageName) // always allow own app
        allowed.add("com.android.launcher") // allow launcher
        allowed.add("com.google.android.launcher")
        allowed.add("com.miui.home")
        allowed.add("com.sec.android.app.launcher")
        allowedApps.forEach { app -> app["packageName"]?.let { allowed.add(it) } }
        allowedPackages = allowed

        // Get session duration
        val durationMinutes = childDoc.getLong("sessionDurationMinutes")?.toInt() ?: 60
        val startTimeMs = (childDoc.getTimestamp("sessionStartTime")?.toDate()?.time) ?: System.currentTimeMillis()
        sessionEndTimeMs = startTimeMs + (durationMinutes * 60 * 1000L)

        sessionActive = true
        updateNotification("🛡️ Study mode ON — blocking restricted apps")
        Log.d(TAG, "Session active. Allowed: $allowedPackages. Ends in ${(sessionEndTimeMs - System.currentTimeMillis()) / 60000}m")
    }

    // ── Polling loop — check foreground app every 500ms ───────────────────────
    private fun startMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = lifecycleScope.launch {
            while (isActive) {
                if (sessionActive) {
                    checkForegroundApp()
                    checkSessionExpiry()
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun checkForegroundApp() {
        val foregroundPackage = getForegroundPackage() ?: return
        // Skip system / own app
        if (foregroundPackage.startsWith("com.android.systemui")) return
        if (foregroundPackage == packageName) return

        if (!allowedPackages.contains(foregroundPackage) && sessionActive) {
            if (lastBlockedApp != foregroundPackage) {
                Log.d(TAG, "BLOCKING: $foregroundPackage")
                lastBlockedApp = foregroundPackage
                blockApp(foregroundPackage)
                reportBlockedAttempt(foregroundPackage)
            }
        } else {
            lastBlockedApp = null
        }
    }

    private fun checkSessionExpiry() {
        if (System.currentTimeMillis() >= sessionEndTimeMs && sessionEndTimeMs > 0) {
            Log.d(TAG, "Session expired by time")
            sessionActive = false
            childId?.let { cid ->
                sessionId?.let { sid ->
                    db.collection("sessions").document(sid)
                        .update("status", "completed", "endTime", FieldValue.serverTimestamp())
                    db.collection("children").document(cid)
                        .update("sessionStatus", "idle", "activeSessionId", null)
                }
            }
            showSessionCompleteNotification()
        }
    }

    private fun getForegroundPackage(): String? {
        return try {
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 5000
            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            var foreground: String? = null
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foreground = event.packageName
                }
            }
            foreground
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground: ${e.message}")
            null
        }
    }

    private fun blockApp(packageName: String) {
        // Launch blocking overlay
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("BLOCKED_APP", packageName)
            putExtra("APP_NAME", AppBlockerUtils.getAppName(this@AppMonitorService, packageName))
        }
        startActivity(intent)
    }

    private fun reportBlockedAttempt(packageName: String) {
        val sid = sessionId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sessionRef = db.collection("sessions").document(sid)
                db.runTransaction { tx ->
                    val snap = tx.get(sessionRef)
                    val attempts = (snap.getLong("blockedAttempts") ?: 0) + 1
                    @Suppress("UNCHECKED_CAST")
                    val appsAttempted = (snap.get("appsAttempted") as? Map<String, Long>)
                        ?.toMutableMap() ?: mutableMapOf()
                    appsAttempted[packageName] = (appsAttempted[packageName] ?: 0) + 1
                    tx.update(sessionRef, mapOf(
                        "blockedAttempts" to attempts,
                        "appsAttempted" to appsAttempted,
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report block: ${e.message}")
            }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Shield Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Study Shield running to monitor apps"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study Shield")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun showSessionCompleteNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1002, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study session complete! 🎉")
            .setContentText("Great focus today. Check your rewards.")
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .build())
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        monitorJob?.cancel()
        sessionListener?.remove()
        Log.d(TAG, "AppMonitorService destroyed")
    }
}
