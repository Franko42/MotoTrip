package com.mototrip

import android.app.Application
import com.mototrip.core.di.AppContainer
import com.mototrip.core.di.DefaultAppContainer
import org.maplibre.android.MapLibre

class MotoTripApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
        MapLibre.getInstance(this)
    }
}
