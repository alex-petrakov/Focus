package me.alex.pet.apps.focus.domain

import java.time.Duration

interface PomodoroSettingsRepository {

    val settings: Pomodoro.Settings

    val workDuration: Duration

    val shortBreakDuration: Duration

    val longBreakDuration: Duration

    val longBreaksAreEnabled: Boolean

    val numberOfSessionsBetweenLongBreaks: Int

    val autoSessionSwitchIsEnabled: Boolean

    fun addObserver(observer: Observer)

    fun removeObserver(observer: Observer)

    interface Observer {
        fun onSettingsChange(settings: Pomodoro.Settings)
    }
}