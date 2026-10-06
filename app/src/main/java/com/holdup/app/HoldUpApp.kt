package com.holdup.app

import android.app.Application
import com.holdup.app.data.preferences.PreferencesManager

class HoldUpApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
    }

    companion object {
        lateinit var instance: HoldUpApp
            private set
    }
}
