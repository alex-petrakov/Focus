package me.alex.pet.apps.focus

import android.app.Application
import me.alex.pet.apps.focus.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class FocusApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@FocusApp)
            modules(appModule)
        }
    }
}