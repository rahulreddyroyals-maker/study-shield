package com.studyshield.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.*
import com.studyshield.R
import com.studyshield.ui.BlockedActivity
import com.studyshield.utils.DeviceIdUtils
import com.studyshield.utils.SessionState
import kotlinx.coroutines.*

class AppMonitorService : LifecycleService() {

    companion object {
        const val TAG = "SSMonitor"
        const val CHANNEL_ID = "ss_monitor"
        const val NOTIF_ID = 1001
        var isRunning = false

        fun start(ctx: Context) {
            val i = Intent(ctx, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, AppMonitorService::class.java))
    }

    private val db = FirebaseFirestore.getInstance()
    private lateinit var usm: UsageStatsManager
    private var pollJob: Job? = null
    private var childListener: ListenerRegistration? = null
    private var lastBlocked: String? = null
    private var lastBlockedTime = 0L

    override fun onCreate() {
        super.onCreate()
        usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIF_ID, buildNotif("Study Shield — standby"))
        val deviceId = DeviceIdUtils.get(this)
        listenFirestore(deviceId)
        startPolling()
        return START_STICKY
    }

    // ── Firestore realtime listener ───────────────────────────────────────────
    private fun listenFirestore(deviceId: String) {
        childListener?.remove()
        db.collection("children")
            .whereEqualTo("deviceId", deviceId)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "Firestore: $err"); return@addSnapshotListener }
                val doc = snap?.documents?.firstOrNull() ?: return@addSnapshotListener
                val status = doc.getString("sessionStatus") ?: "idle"
                val sessionId = doc.getString("activeSessionId")

                if (status == "active" && sessionId != null) {
                    @Suppress("UNCHECKED_CAST")
                    val allowedApps = (doc.get("allowedApps") as? List<Map<String, String>>) ?: emptyList()
                    val allowed = mutableSetOf(
                        packageName,
                        "com.android.launcher3", "com.google.android.launcher",
                        "com.miui.home", "com.sec.android.app.launcher",
                        "com.huawei.android.launcher", "com.oppo.launcher",
                        "com.android.settings", "com.android.systemui",
                    )
                    allowedApps.forEach { it["packageName"]?.let { pkg -> allowed.add(pkg) } }

                    val durMin = doc.getLong("sessionDurationMinutes")?.toInt() ?: 60
                    val startMs = doc.getTimestamp("sessionStartTime")?.toDate()?.time ?: System.currentTimeMillis()

                    SessionState.update(
                        sessionId = sessionId,
                        childId = doc.id,
                        allowedPackages = allowed,
                        endTimeMs = startMs + durMin * 60_000L
                    )
                    updateNotif("🛡️ Study mode ON · blocking restricted apps")
                    Log.d(TAG, "Session active. ${allowed.size} apps allowed.")
                } else {
                    SessionState.clear()
                    updateNotif("Study Shield — standby")
                    Log.d(TAG, "No active session")
                }
            }
    }

    // ── Polling loop every 500ms ──────────────────────────────────────────────
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                val session = SessionState.current
                if (session != null) {
                    checkForeground(session)
                    checkExpiry(session)
                }
                delay(500)
            }
        }
    }

    private fun checkForeground(session: SessionState.Session) {
        val pkg = getForeground() ?: return
        if (pkg.startsWith("com.android.systemui")) return
        if (pkg == packageName) return
        if (session.allowedPackages.contains(pkg)) { lastBlocked = null; return }

        val now = System.currentTimeMillis()
        // Debounce — don't spam block screen for same app within 3s
        if (pkg == lastBlocked && now - lastBlockedTime < 3000) return

        lastBlocked = pkg
        lastBlockedTime = now
        Log.d(TAG, "BLOCKING: $pkg")

        // Show block screen
        startActivity(Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("PKG", pkg)
            putExtra("APP_NAME", getAppName(pkg))
        })

        // Report to Firestore
        reportBlock(session.sessionId, pkg)
    }

    private fun checkExpiry(session: SessionState.Session) {
        if (System.currentTimeMillis() < session.endTimeMs) return
        Log.d(TAG, "Session expired by timer")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.collection("sessions").document(session.sessionId)
                    .update(mapOf("status" to "completed", "endTime" to FieldValue.serverTimestamp()))
                db.collection("children").document(session.childId)
                    .update(mapOf("sessionStatus" to "idle", "activeSessionId" to null))
                SessionState.clear()
                showSessionDoneNotif()
            } catch (e: Exception) { Log.e(TAG, "Expiry update failed: $e") }
        }
    }

    private fun getForeground(): String? {
        return try {
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 3000, now)
            val event = UsageEvents.Event()
            var last: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = event.packageName
            }
            last
        } catch (e: Exception) { null }
    }

    private fun reportBlock(sessionId: String, pkg: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ref = db.collection("sessions").document(sessionId)
                db.runTransaction { tx ->
                    val snap = tx.get(ref)
                    val attempts = (snap.getLong("blockedAttempts") ?: 0) + 1
                    @Suppress("UNCHECKED_CAST")
                    val map = ((snap.get("appsAttempted") as? Map<String, Long>)?.toMutableMap()) ?: mutableMapOf()
                    map[pkg] = (map[pkg] ?: 0) + 1
                    tx.update(ref, "blockedAttempts", attempts, "appsAttempted", map)
                }
            } catch (e: Exception) { Log.e(TAG, "reportBlock: $e") }
        }
    }

    private fun getAppName(pkg: String) = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg.substringAfterLast(".") }

    // ── Notifications ─────────────────────────────────────────────────────────
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Study Shield", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotif(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Study Shield")
        .setContentText(text)
        .setSmallIcon(R.drawable.ic_shield)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun updateNotif(text: String) =
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif(text))

    private fun showSessionDoneNotif() {
        val ch = NotificationChannel("ss_done", "Session Complete", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        val n = NotificationCompat.Builder(this, "ss_done")
            .setContentTitle("Study session complete! 🎉")
            .setContentText("Great work! Check your rewards.")
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(1002, n)
    }

    override fun onDestroy() { super.onDestroy(); isRunning = false; pollJob?.cancel(); childListener?.remove() }
}
