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
        @Volatile var isRunning = false

        fun start(context: Context) {
            val i = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) =
            context.stopService(Intent(context, AppMonitorService::class.java))
    }

    private val db = FirebaseFirestore.getInstance()
    private lateinit var usm: UsageStatsManager
    private var pollJob: Job? = null
    private var childListener: ListenerRegistration? = null
    private var lastBlockedPkg: String? = null
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
        listenFirestore(DeviceIdUtils.get(this))
        startPolling()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        pollJob?.cancel()
        childListener?.remove()
        SessionState.clear()
    }

    private fun listenFirestore(deviceId: String) {
        childListener?.remove()
        childListener = db.collection("children")
            .whereEqualTo("deviceId", deviceId)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "Firestore: $err"); return@addSnapshotListener }
                val doc = snap?.documents?.firstOrNull() ?: run {
                    SessionState.clear(); return@addSnapshotListener
                }
                val status = doc.getString("sessionStatus") ?: "idle"
                val sessionId = doc.getString("activeSessionId")

                if (status == "active" && sessionId != null) {
                    @Suppress("UNCHECKED_CAST")
                    val apps = (doc.get("allowedApps") as? List<Map<String, Any>>) ?: emptyList()
                    val allowed = mutableSetOf(
                        packageName,
                        "com.android.launcher3", "com.android.launcher",
                        "com.google.android.launcher", "com.miui.home",
                        "com.sec.android.app.launcher", "com.huawei.android.launcher",
                        "com.android.settings", "com.android.systemui",
                        "com.android.dialer", "com.android.phone"
                    )
                    apps.forEach { (it["packageName"] as? String)?.let { p -> allowed.add(p) } }

                    val durMin = (doc.getLong("sessionDurationMinutes") ?: 60).toInt()
                    val startMs = doc.getTimestamp("sessionStartTime")?.toDate()?.time
                        ?: System.currentTimeMillis()

                    SessionState.update(sessionId, doc.id, allowed, startMs + durMin * 60_000L)
                    updateNotif("🛡️ Study mode ON · blocking restricted apps")
                } else {
                    SessionState.clear()
                    lastBlockedPkg = null
                    updateNotif("Study Shield — standby")
                }
            }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                SessionState.current?.let { checkForeground(it); checkExpiry(it) }
                delay(500)
            }
        }
    }

    private fun checkForeground(s: SessionState.Session) {
        val pkg = getForeground() ?: return
        if (pkg.startsWith("com.android.systemui") || pkg == packageName) return
        if (s.allowedPackages.contains(pkg)) { lastBlockedPkg = null; return }

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockedTime < 2000L) return
        lastBlockedPkg = pkg; lastBlockedTime = now

        startActivity(Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("APP_NAME", getAppName(pkg))
        })
        reportBlock(s.sessionId, pkg)
    }

    private fun checkExpiry(s: SessionState.Session) {
        if (System.currentTimeMillis() < s.endTimeMs) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.collection("sessions").document(s.sessionId)
                    .update("status", "completed", "endTime", FieldValue.serverTimestamp())
                db.collection("children").document(s.childId)
                    .update("sessionStatus", "idle", "activeSessionId", null)
                SessionState.clear()
                showDoneNotif()
            } catch (e: Exception) { Log.e(TAG, "Expiry: $e") }
        }
    }

    private fun getForeground(): String? {
        return try {
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 3000, now)
            val e = UsageEvents.Event(); var last: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
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

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Study Shield", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false); ch.enableVibration(false); ch.setSound(null, null)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotif(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Study Shield").setContentText(text)
        .setSmallIcon(R.drawable.ic_shield).setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW).setSilent(true).build()

    private fun updateNotif(text: String) =
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif(text))

    private fun showDoneNotif() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel("ss_done", "Done", NotificationManager.IMPORTANCE_DEFAULT))
        getSystemService(NotificationManager::class.java).notify(1002,
            NotificationCompat.Builder(this, "ss_done")
                .setContentTitle("Study session complete! 🎉")
                .setContentText("Great work! Check your rewards.")
                .setSmallIcon(R.drawable.ic_shield).setAutoCancel(true).build())
    }
}
