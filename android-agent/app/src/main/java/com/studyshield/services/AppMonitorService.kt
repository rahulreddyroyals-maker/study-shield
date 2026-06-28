package com.studyshield.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.studyshield.R
import com.studyshield.ui.BlockedActivity
import com.studyshield.utils.DeviceIdUtils
import com.studyshield.utils.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppMonitorService : LifecycleService() {

    companion object {
        const val TAG = "SSMonitor"
        const val CHANNEL_ID = "ss_monitor"
        const val NOTIF_ID = 1001
        const val POLL_MS = 500L

        @Volatile var isRunning = false

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
    private lateinit var usageStatsManager: UsageStatsManager
    private var pollJob: Job? = null
    private var childListener: ListenerRegistration? = null
    private var lastBlockedPkg: String? = null
    private var lastBlockedTime = 0L

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        isRunning = true
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIF_ID, buildNotification("Study Shield — standby"))
        val deviceId = DeviceIdUtils.get(this)
        Log.i(TAG, "Starting with deviceId=$deviceId")
        startFirestoreListener(deviceId)
        startPollingLoop()
        return START_STICKY  // Restart automatically if killed
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        pollJob?.cancel()
        childListener?.remove()
        SessionState.clear()
        Log.i(TAG, "Service destroyed")
    }

    // ── Firestore realtime listener ───────────────────────────────────────────

    private fun startFirestoreListener(deviceId: String) {
        childListener?.remove()
        childListener = db.collection("children")
            .whereEqualTo("deviceId", deviceId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore listener error: ${error.message}")
                    return@addSnapshotListener
                }

                val doc = snapshot?.documents?.firstOrNull()
                if (doc == null) {
                    Log.w(TAG, "No child document found for deviceId=$deviceId")
                    SessionState.clear()
                    updateNotification("Study Shield — waiting for setup")
                    return@addSnapshotListener
                }

                val status = doc.getString("sessionStatus") ?: "idle"
                val activeSessionId = doc.getString("activeSessionId")

                if (status == "active" && activeSessionId != null) {
                    // Build allowed package set
                    @Suppress("UNCHECKED_CAST")
                    val allowedApps = (doc.get("allowedApps") as? List<Map<String, Any>>) ?: emptyList()

                    val allowed = mutableSetOf<String>().apply {
                        add(packageName)  // Always allow own app
                        // Common launchers
                        addAll(listOf(
                            "com.android.launcher3",
                            "com.android.launcher",
                            "com.google.android.launcher",
                            "com.miui.home",
                            "com.sec.android.app.launcher",
                            "com.huawei.android.launcher",
                            "com.oppo.launcher",
                            "com.vivo.launcher",
                            "com.oneplus.launcher",
                            "com.realme.launcher",
                        ))
                        // System essentials
                        addAll(listOf(
                            "com.android.settings",
                            "com.android.systemui",
                            "com.android.dialer",      // Phone
                            "com.android.phone",
                            "com.android.incallui",
                        ))
                        // Apps from Firestore
                        allowedApps.forEach { app ->
                            (app["packageName"] as? String)?.let { add(it) }
                        }
                    }

                    val durationMinutes = (doc.getLong("sessionDurationMinutes") ?: 60).toInt()
                    val startMs = doc.getTimestamp("sessionStartTime")?.toDate()?.time
                        ?: System.currentTimeMillis()
                    val endMs = startMs + (durationMinutes * 60_000L)

                    SessionState.update(
                        sessionId = activeSessionId,
                        childId = doc.id,
                        allowedPackages = allowed,
                        endTimeMs = endMs
                    )

                    updateNotification("🛡️ Study mode ON · ${allowed.size} apps allowed")
                    Log.i(TAG, "Session active: $activeSessionId | ends in ${(endMs - System.currentTimeMillis()) / 60000}m")
                } else {
                    SessionState.clear()
                    lastBlockedPkg = null
                    updateNotification("Study Shield — standby")
                    Log.i(TAG, "No active session (status=$status)")
                }
            }
    }

    // ── Polling loop ──────────────────────────────────────────────────────────

    private fun startPollingLoop() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                val session = SessionState.current
                if (session != null) {
                    checkForegroundApp(session)
                    checkSessionExpiry(session)
                }
                delay(POLL_MS)
            }
        }
    }

    private fun checkForegroundApp(session: SessionState.Session) {
        val foregroundPkg = getForegroundPackage() ?: return

        // Skip system UI
        if (foregroundPkg.startsWith("com.android.systemui")) return
        if (foregroundPkg == packageName) return

        // Is it allowed?
        if (session.allowedPackages.contains(foregroundPkg)) {
            lastBlockedPkg = null
            return
        }

        // Debounce — same app blocked within 2 seconds, skip
        val now = System.currentTimeMillis()
        if (foregroundPkg == lastBlockedPkg && now - lastBlockedTime < 2000L) return

        lastBlockedPkg = foregroundPkg
        lastBlockedTime = now

        Log.i(TAG, "BLOCKING: $foregroundPkg")

        // Launch block screen
        val blockIntent = Intent(this@AppMonitorService, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("BLOCKED_PKG", foregroundPkg)
            putExtra("APP_NAME", getAppDisplayName(foregroundPkg))
        }
        startActivity(blockIntent)

        // Report to Firestore asynchronously
        reportBlockedAttempt(session.sessionId, foregroundPkg)
    }

    private fun checkSessionExpiry(session: SessionState.Session) {
        if (System.currentTimeMillis() < session.endTimeMs) return

        Log.i(TAG, "Session expired — ending automatically")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.collection("sessions").document(session.sessionId)
                    .update(
                        "status", "completed",
                        "endTime", FieldValue.serverTimestamp()
                    )
                db.collection("children").document(session.childId)
                    .update(
                        "sessionStatus", "idle",
                        "activeSessionId", null
                    )
                showSessionCompleteNotification()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end session: ${e.message}")
            }
        }
    }

    // ── UsageStats ────────────────────────────────────────────────────────────

    private fun getForegroundPackage(): String? {
        return try {
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 3000L
            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            var lastForeground: String? = null
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForeground = event.packageName
                }
            }
            lastForeground
        } catch (e: Exception) {
            Log.e(TAG, "getForegroundPackage error: ${e.message}")
            null
        }
    }

    // ── Firestore reporting ───────────────────────────────────────────────────

    private fun reportBlockedAttempt(sessionId: String, packageName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sessionRef = db.collection("sessions").document(sessionId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(sessionRef)
                    val currentAttempts = snapshot.getLong("blockedAttempts") ?: 0L
                    @Suppress("UNCHECKED_CAST")
                    val appsMap = (snapshot.get("appsAttempted") as? Map<String, Long>)
                        ?.toMutableMap() ?: mutableMapOf()
                    appsMap[packageName] = (appsMap[packageName] ?: 0L) + 1L
                    transaction.update(
                        sessionRef,
                        "blockedAttempts", currentAttempts + 1,
                        "appsAttempted", appsMap
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "reportBlockedAttempt error: ${e.message}")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getAppDisplayName(pkg: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg.substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
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
                description = "Keeps Study Shield running in background"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study Shield")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(text))
    }

    private fun showSessionCompleteNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "ss_complete", "Session Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val n = NotificationCompat.Builder(this, "ss_complete")
            .setContentTitle("Study session complete! 🎉")
            .setContentText("Great work today. Check your rewards.")
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(1002, n)
    }
}
