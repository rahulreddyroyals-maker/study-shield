package com.studyshield.app

import android.app.Application
import androidx.multidex.MultiDex
import android.content.Context

class StudyShieldApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
