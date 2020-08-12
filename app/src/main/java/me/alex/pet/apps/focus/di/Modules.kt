package me.alex.pet.apps.focus.di

import me.alex.pet.apps.focus.data.AndroidClock
import me.alex.pet.apps.focus.data.Prefs
import me.alex.pet.apps.focus.domain.Clock
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.PomodoroConfigurationRepository
import me.alex.pet.apps.focus.presentation.timer.TimerModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { TimerModel(androidApplication(), get()) }

    single { Pomodoro(get(), get()) }

    single<PomodoroConfigurationRepository> { Prefs(get()) }

    single<Clock> { AndroidClock() }
}