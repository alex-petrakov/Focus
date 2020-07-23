package me.alex.pet.apps.focus.di

import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.presentation.timer.TimerModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { TimerModel(androidApplication(), get()) }

    single {
        Pomodoro(
                10,
                2,
                5,
                true,
                1,
                false
        )
    }
}