package com.example.seachem_dosing

import android.app.Application
import com.example.seachem_dosing.di.appModule
import com.example.seachem_dosing.di.dataModule
import com.example.seachem_dosing.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class — boots Koin DI graph.
 * Registered in AndroidManifest.xml via android:name=".SeachemDosingApp".
 *
 * Module composition:
 *   appModule    — app-scoped state (settings, theme, locale).
 *   dataModule   — Room database + DAOs + Repository implementations.
 *   domainModule — UseCase classes (factory-scoped).
 *
 * AI module will be added when Phase 7 wires GeminiClient / LocalLlmClient.
 */
class SeachemDosingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
            androidContext(this@SeachemDosingApp)
            modules(appModule, dataModule, domainModule)
        }
    }
}
