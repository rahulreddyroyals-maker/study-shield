package com.studyshield.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import com.studyshield.services.AppMonitorService

class BlockedActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var relaunchRunnable: Runnable? = null
    private var lastBackPress = 0L

    companion object {
        private val QUOTES = listOf(
            "\"Success is the sum of small efforts, repeated day in and day out.\"",
            "\"The secret of getting ahead is getting started.\"",
            "\"Focus on being productive instead of busy.\"",
            "\"Small daily improvements lead to stunning long-term results.\"",
            "\"You don't have to be great to start, but you have to start to be great.\"",
            "\"Discipline is choosing between what you want now and what you want most.\""
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val appName = intent.getStringExtra("APP_NAME") ?: "This app"

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF0D1B3E.toInt()) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 80, 48, 80)
        }

        // Shield
        root.addView(TextView(this).apply {
            text = "🛡️"
            textSize = 80f
            gravity = Gravity.CENTER
        })

        // Title
        root.addView(TextView(this).apply {
            text = "App Blocked"
            textSize = 28f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 16; lp.bottomMargin = 8; layoutParams = lp
        })

        // App name
        root.addView(TextView(this).apply {
            text = "\"$appName\" is not allowed\nduring study time"
            textSize = 15f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            lineSpacingExtra = 4f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 24; layoutParams = lp
        })

        // Active badge
        root.addView(TextView(this).apply {
            text = "  🛡️  Study Shield is protecting your focus  "
            textSize = 12f
            setTextColor(0xFF22C55E.toInt())
            setPadding(20, 10, 20, 10)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0x1422C55E)
                cornerRadius = 40f
                setStroke(1, 0xFF22C55E.toInt())
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = 40; layoutParams = lp
        })

        // Back to studying button
        root.addView(Button(this).apply {
            text = "📚  Back to studying"
            textSize = 16f; isAllCaps = false
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                colors = intArrayOf(0xFF22C55E.toInt(), 0xFF0EA5E9.toInt())
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = 16f
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 12; layoutParams = lp; setPadding(0, 48, 0, 48)
            setOnClickListener { goHome() }
        })

        // Emergency button
        root.addView(Button(this).apply {
            text = "📞  Emergency call"
            textSize = 14f; isAllCaps = false
            setTextColor(0xFFEF4444.toInt())
            setBackgroundColor(0x00000000)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 48; layoutParams = lp
            setOnClickListener { handleEmergency() }
        })

        // Motivational quote
        root.addView(TextView(this).apply {
            text = QUOTES.random()
            textSize = 12f
            setTextColor(0xFF334155.toInt())
            gravity = Gravity.CENTER
            lineSpacingExtra = 4f
            fontFeatureSettings = "\"smcp\""
        })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun handleEmergency() {
        Toast.makeText(this, "Emergency override — parent notified!", Toast.LENGTH_LONG).show()
        // TODO: Trigger FCM to parent via Firebase Cloud Function
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 1000L) {
            goHome()
        } else {
            lastBackPress = now
            Toast.makeText(this, "Tap back again to go to home screen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // If student swipes away, relaunch after 250ms
        if (AppMonitorService.isRunning) {
            relaunchRunnable = Runnable {
                if (!isFinishing) {
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }.also { handler.postDelayed(it, 250L) }
        }
    }

    override fun onResume() {
        super.onResume()
        relaunchRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        relaunchRunnable?.let { handler.removeCallbacks(it) }
    }
}
