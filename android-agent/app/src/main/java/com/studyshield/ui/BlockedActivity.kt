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
    private var relaunchR: Runnable? = null
    private var lastBack = 0L
    private val quotes = listOf(
        "\"Success is the sum of small efforts, repeated day in and day out.\"",
        "\"The secret of getting ahead is getting started.\"",
        "\"Focus on being productive instead of busy.\"",
        "\"Discipline is choosing what you want most over what you want now.\""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        val appName = intent.getStringExtra("APP_NAME") ?: "This app"
        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF0D1B3E.toInt()) }
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(48,80,48,80) }

        root.addView(TextView(this).apply { text="🛡️"; textSize=80f; gravity=Gravity.CENTER })
        root.addView(TextView(this).apply { text="App Blocked"; textSize=28f; setTextColor(0xFFFFFFFF.toInt()); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER; val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.topMargin=16; lp.bottomMargin=8; layoutParams=lp })
        root.addView(TextView(this).apply { text="\"$appName\" is not allowed\nduring study time"; textSize=15f; setTextColor(0xFF94A3B8.toInt()); gravity=Gravity.CENTER; lineSpacingExtra=4f; val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.bottomMargin=24; layoutParams=lp })
        root.addView(TextView(this).apply { text="  🛡️  Study Shield is protecting your focus  "; textSize=12f; setTextColor(0xFF22C55E.toInt()); setPadding(20,10,20,10); gravity=Gravity.CENTER
            background=GradientDrawable().apply { setColor(0x1422C55E); cornerRadius=40f; setStroke(1,0xFF22C55E.toInt()) }
            val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.gravity=Gravity.CENTER_HORIZONTAL; lp.bottomMargin=40; layoutParams=lp })

        root.addView(Button(this).apply {
            text="📚  Back to studying"; textSize=16f; isAllCaps=false; setTextColor(0xFFFFFFFF.toInt())
            background=GradientDrawable().apply { colors=intArrayOf(0xFF22C55E.toInt(),0xFF0EA5E9.toInt()); orientation=GradientDrawable.Orientation.LEFT_RIGHT; cornerRadius=16f }
            val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.bottomMargin=12; layoutParams=lp; setPadding(0,48,0,48)
            setOnClickListener { goHome() }
        })
        root.addView(Button(this).apply {
            text="📞  Emergency call"; textSize=14f; isAllCaps=false; setTextColor(0xFFEF4444.toInt()); setBackgroundColor(0)
            val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.bottomMargin=48; layoutParams=lp
            setOnClickListener { Toast.makeText(this@BlockedActivity,"Emergency — parent notified!",Toast.LENGTH_LONG).show(); finish() }
        })
        root.addView(TextView(this).apply { text=quotes.random(); textSize=12f; setTextColor(0xFF334155.toInt()); gravity=Gravity.CENTER; lineSpacingExtra=4f })

        scroll.addView(root); setContentView(scroll)
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME); flags=Intent.FLAG_ACTIVITY_NEW_TASK })
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now=System.currentTimeMillis()
        if (now-lastBack<1000L) goHome() else { lastBack=now; Toast.makeText(this,"Tap back again to go home",Toast.LENGTH_SHORT).show() }
    }

    override fun onPause() {
        super.onPause()
        if (AppMonitorService.isRunning) {
            relaunchR = Runnable { if (!isFinishing) startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.also { handler.postDelayed(it,250L) }
        }
    }
    override fun onResume() { super.onResume(); relaunchR?.let { handler.removeCallbacks(it) } }
    override fun onDestroy() { super.onDestroy(); relaunchR?.let { handler.removeCallbacks(it) } }
}
