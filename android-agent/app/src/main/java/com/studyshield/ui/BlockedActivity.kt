package com.studyshield.ui

import android.app.Activity
import android.content.Intent
import android.os.*
import android.view.WindowManager
import android.widget.*
import com.studyshield.services.AppMonitorService

class BlockedActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastBack = 0L
    private var relaunchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        val appName = intent.getStringExtra("APP_NAME") ?: "This app"
        val quotes = listOf(
            "\"Success is the sum of small efforts, repeated day in and day out.\"",
            "\"Focus on being productive instead of busy.\"",
            "\"The secret of getting ahead is getting started.\"",
            "\"Small daily improvements lead to stunning results.\"",
        )
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF0D1B3E.toInt()); setPadding(40,80,40,80)
        }
        root.addView(tv("🛡️", 72f, 0xFFFFFFFF.toInt()))
        root.addView(tv("App Blocked", 28f, 0xFFFFFFFF.toInt(), bold=true, topPad=24))
        root.addView(tv("\"$appName\" is not allowed\nduring study time", 15f, 0xFF94A3B8.toInt(), topPad=12))
        root.addView(tv("🛡️  Study Shield is protecting your focus", 12f, 0xFF22C55E.toInt(), topPad=24))
        root.addView(btn("📚  Back to studying", topPad=48) { goHome() })
        root.addView(btn("📞  Emergency call", isGhost=true, topPad=12) { handleEmergency() })
        root.addView(tv(quotes.random(), 12f, 0xFF334155.toInt(), topPad=48, center=true))
        setContentView(root)
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean=false, topPad: Int=0, center: Boolean=true): TextView {
        return TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            if (center) gravity = android.view.Gravity.CENTER
            lineSpacingExtra = 4f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = topPad; layoutParams = lp
        }
    }

    private fun btn(label: String, isGhost: Boolean=false, topPad: Int=0, onClick: () -> Unit): Button {
        return Button(this).apply {
            text=label; textSize=15f; isAllCaps=false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin=topPad; layoutParams=lp; setPadding(0,40,0,40)
            if (isGhost) {
                setTextColor(0xFFEF4444.toInt()); setBackgroundColor(0)
            } else {
                setTextColor(0xFFFFFFFF.toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    colors = intArrayOf(0xFF22C55E.toInt(),0xFF0EA5E9.toInt())
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                    cornerRadius = 14f
                }
            }
            setOnClickListener { onClick() }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME); flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun handleEmergency() {
        android.widget.Toast.makeText(this, "Emergency — parent notified!", android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBack < 1000) goHome() else {
            lastBack = now
            android.widget.Toast.makeText(this,"Tap back again to go home",android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Re-show if student tries to swipe away
        relaunchRunnable = Runnable {
            if (!isFinishing && AppMonitorService.isRunning) {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }.also { handler.postDelayed(it, 250) }
    }

    override fun onResume() { super.onResume(); relaunchRunnable?.let { handler.removeCallbacks(it) } }
    override fun onDestroy() { super.onDestroy(); relaunchRunnable?.let { handler.removeCallbacks(it) } }
}
