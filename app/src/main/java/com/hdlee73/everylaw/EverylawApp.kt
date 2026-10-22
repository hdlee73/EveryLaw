package com.hdlee73.everylaw

import android.app.Application
import com.hdlee73.everylaw.di.AppContainer

class EverylawApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
