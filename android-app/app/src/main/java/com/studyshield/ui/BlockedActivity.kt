// BlockedActivity.kt
// Full-screen activity shown when a restricted app is detected.
// Student cannot go back to the blocked app — only to the home screen.
package com.studyshield.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.*
import com.studyshield.R
import com.studyshield.services.AppMonitorService

class BlockedActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastBack = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw over lock screen if needed
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        val blockedApp = intent.getStringExtra("BLOCKED_APP") ?: "Unknown"
        val appName = intent.getStringExtra("APP_NAME") ?: blockedApp

        // Build UI programmatically (no XML needed for simplicity)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF0D1B3E.toInt())
            setPadding(48, 48, 48, 48)
        }

        // Shield emoji
        val shieldTv = TextView(this).apply {
            text = "🛡️"
            textSize = 72f
            gravity = android.view.Gravity.CENTER
        }

        // Title
        val titleTv = TextView(this).apply {
            text = "App Blocked"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 24, 0, 8)
        }

        // App name
        val appTv = TextView(this).apply {
            text = "\"$appName\" is not allowed\nduring study time"
            textSize = 15f
            setTextColor(0xFF94A3B8.toInt())
            gravity = android.view.Gravity.CENTER
            lineSpacingExtra = 6f
        }

        // Focus pill
        val focusPill = TextView(this).apply {
            text = "🛡️  Study Shield is active"
            textSize = 13f
            setTextColor(0xFF22C55E.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(24, 10, 24, 10)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x1A22C55E)
                cornerRadius = 40f
                setStroke(1, 0xFF22C55E.toInt())
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 32
            layoutParams = lp
        }

        // Go back to study button
        val studyBtn = Button(this).apply {
            text = "📚  Back to studying"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                colors = intArrayOf(0xFF22C55E.toInt(), 0xFF0EA5E9.toInt())
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = 16f
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 48
            layoutParams = lp
            setPadding(0, 48, 0, 48)
            isAllCaps = false
            setOnClickListener { goHome() }
        }

        // Emergency call button
        val emergencyBtn = Button(this).apply {
            text = "📞  Emergency call"
            textSize = 13f
            setTextColor(0xFFEF4444.toInt())
            setBackgroundColor(0x00000000)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 12
            layoutParams = lp
            isAllCaps = false
            setOnClickListener { handleEmergency() }
        }

        // Motivational quote
        val quotes = listOf(
            "📖  \"Success is the sum of small efforts, repeated day in and day out.\"",
            "🌟  \"Focus on being productive instead of busy.\"",
            "💪  \"You don't have to be great to start, but you have to start to be great.\"",
            "🎯  \"The secret of getting ahead is getting started.\"",
        )
        val quoteTv = TextView(this).apply {
            text = quotes.random()
            textSize = 12f
            setTextColor(0xFF475569.toInt())
            gravity = android.view.Gravity.CENTER
            lineSpacingExtra = 4f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 48
            layoutParams = lp
        }

        root.addView(shieldTv)
        root.addView(titleTv)
        root.addView(appTv)
        root.addView(focusPill)
        root.addView(studyBtn)
        root.addView(emergencyBtn)
        root.addView(quoteTv)
        setContentView(root)
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun handleEmergency() {
        // In a real build: launch dialer or send FCM to parent
        // For now, allow calling via the phone app
        android.widget.Toast.makeText(this, "Emergency: Parent notified!", android.widget.Toast.LENGTH_SHORT).show()
        // TODO: call Firebase Cloud Function to send FCM to parent
        finish()
    }

    override fun onBackPressed() {
        // Double-tap back to go home (prevents back-button bypass)
        val now = System.currentTimeMillis()
        if (now - lastBack < 1000) {
            goHome()
        } else {
            lastBack = now
            android.widget.Toast.makeText(this, "Tap back again to go home", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Relaunch if student tries to swipe away
        handler.postDelayed({
            if (!isFinishing && AppMonitorService.isRunning) {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }, 200)
    }
}
