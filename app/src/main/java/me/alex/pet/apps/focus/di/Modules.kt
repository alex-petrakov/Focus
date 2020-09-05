package me.alex.pet.apps.focus.di

import me.alex.pet.apps.focus.common.extensions.minutes
import me.alex.pet.apps.focus.data.AndroidClock
import me.alex.pet.apps.focus.data.NotificationPrefs
import me.alex.pet.apps.focus.data.NotificationSoundPlayer
import me.alex.pet.apps.focus.data.PomodoroPrefs
import me.alex.pet.apps.focus.domain.*
import me.alex.pet.apps.focus.presentation.timer.TimerModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { TimerModel(androidApplication(), get()) }

    single { Pomodoro(get(), get()) }

    single<PomodoroSettings> { PomodoroPrefs(androidContext()) }

    single<Timer> { SessionTimer(get(), 1.minutes) }

    single<Clock> { AndroidClock() }

    single { NotificationSoundPlayer(androidContext(), get()) }

    single { NotificationPrefs(androidContext()) }
}