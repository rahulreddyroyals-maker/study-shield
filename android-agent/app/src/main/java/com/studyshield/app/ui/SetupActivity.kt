package com.studyshield.app.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private var step = 0

    data class Step(
        val emoji: String,
        val title: String,
        val desc: String,
        val btnLabel: String,
        val action: SetupActivity.() -> Unit,
        val check: SetupActivity.() -> Boolean
    )

    private val steps by lazy {
        listOf(
            Step("📊", "Usage access",
                "Allow Study Shield to detect which app is open.\n\n1. Tap button below\n2. Find Study Shield\n3. Enable Permit usage access",
                "Open Usage Access Settings",
                { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                { hasUsageAccess() }
            ),
            Step("🪟", "Overlay permission",
                "Allows block screen to appear over other apps.\n\n1. Tap button below\n2. Find Study Shield\n3. Enable Allow display over other apps",
                "Grant Overlay Permission",
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                },
                { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this) }
            ),
            Step("♿", "Accessibility",
                "Secondary app detection layer.\n\n1. Tap button below\n2. Find Study Shield\n3. Toggle ON → tap OK",
                "Open Accessibility Settings",
                { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                { false }
            ),
            Step("✅", "All set!",
                "Study Shield is ready.\n\nShare the Device ID with the parent to link this device.\n\nParent controls sessions from studyshield-ai.web.app",
                "Open Study Shield",
                { startActivity(Intent(this, MainActivity::class.java)); finish() },
                { true }
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        show(0)
    }

    private fun show(idx: Int) {
        step = idx
        val s = steps.getOrNull(idx) ?: return

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF0D1B3E.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 80, 48, 80)
        }

        // Step counter
        root.addView(TextView(this).apply {
            text = "Step ${idx + 1} of ${steps.size}"
            textSize = 12f; setTextColor(0xFF64748B.toInt()); gravity = Gravity.CENTER
        })

        // Progress bar
        val pbLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        pbLp.topMargin = 12; pbLp.bottomMargin = 32
        val pb = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutParams = pbLp
        }
        repeat(steps.size) { i ->
            val segLp = LinearLayout.LayoutParams(0, 6, 1f)
            segLp.setMargins(3, 0, 3, 0)
            pb.addView(View(this).apply {
                layoutParams = segLp
                background = GradientDrawable().apply {
                    cornerRadius = 3f
                    setColor(if (i <= idx) 0xFF22C55E.toInt() else 0xFF1E3A5F.toInt())
                }
            })
        }
        root.addView(pb)

        // Emoji
        val emojiLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        emojiLp.bottomMargin = 8
        root.addView(TextView(this).apply {
            text = s.emoji; textSize = 64f; gravity = Gravity.CENTER; layoutParams = emojiLp
        })

        // Title
        val titleLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        titleLp.bottomMargin = 16
        root.addView(TextView(this).apply {
            text = s.title; textSize = 24f; setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; layoutParams = titleLp
        })

        // Description
        val descLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        descLp.bottomMargin = 40
        root.addView(TextView(this).apply {
            text = s.desc; textSize = 14f; setTextColor(0xFF94A3B8.toInt())
            lineSpacingExtra = 5f; layoutParams = descLp
        })

        // Action button
        val actionLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        actionLp.bottomMargin = 12
        root.addView(Button(this).apply {
            text = s.btnLabel; textSize = 15f; isAllCaps = false; setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                colors = intArrayOf(0xFF22C55E.toInt(), 0xFF0EA5E9.toInt())
                orientation = GradientDrawable.Orientation.LEFT_RIGHT; cornerRadius = 14f
            }
            layoutParams = actionLp; setPadding(0, 44, 0, 44)
            setOnClickListener { s.action(this@SetupActivity) }
        })

        // Skip / Next button
        if (idx < steps.size - 1) {
            root.addView(Button(this).apply {
                text = if (idx == 2) "I've enabled this →" else "Next →"
                textSize = 14f; isAllCaps = false
                setTextColor(0xFF22C55E.toInt()); setBackgroundColor(0)
                setOnClickListener { show(idx + 1) }
            })
        }

        scroll.addView(root); setContentView(scroll)
    }

    private fun hasUsageAccess(): Boolean = try {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) { false }

    override fun onResume() {
        super.onResume()
        steps.getOrNull(step)?.let {
            if (it.check(this) && step < steps.size - 1) show(step + 1)
        }
    }
}
