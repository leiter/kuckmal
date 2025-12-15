package com.mediathekview.android

import android.app.Application
import com.mediathekview.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for MediathekView
 * Initializes Koin dependency injection
 */
class OerFinderApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start Koin
        startKoin {
            // Log Koin into Android logger
            androidLogger(Level.ERROR) // Set to Level.DEBUG for development

            // Reference Android context
            androidContext(this@OerFinderApplication)

            // Load modules
            modules(appModule)
        }
    }
}
