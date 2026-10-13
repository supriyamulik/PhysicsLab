package com.physicslab.app

import android.app.Application

/** Applies the saved theme mode as early as possible, before any Activity is created. */
class PhysicsLabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.apply(ThemeManager.getMode(this))
    }
}
