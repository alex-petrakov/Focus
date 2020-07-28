package me.alex.pet.apps.focus

import android.app.Application
import me.alex.pet.apps.focus.di.appModule
import me.alex.pet.apps.focus.presentation.notificationservice.Notifications
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class FocusApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@FocusApp)
            modules(appModule)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Notifications(this).createNotificationChannels()
    }
}