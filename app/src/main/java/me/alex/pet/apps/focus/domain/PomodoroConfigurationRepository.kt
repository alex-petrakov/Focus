package me.alex.pet.apps.focus.domain

import java.time.Duration

interface PomodoroConfigurationRepository {

    val workDuration: Duration

    val shortBreakDuration: Duration

    val longBreakDuration: Duration

    val longBreaksAreEnabled: Boolean

    val numberOfSessionsBetweenLongBreaks: Int

    val autoSessionSwitchIsEnabled: Boolean

    fun addPomodoroConfigurationObserver(observer: PomodoroConfigurationObserver)

    fun removePomodoroConfigurationObserver(observer: PomodoroConfigurationObserver)

    interface PomodoroConfigurationObserver {
        fun onWorkDurationChange(workDuration: Duration)

        fun onShortBreakDurationChange(shortBreakDuration: Duration)

        fun onLongBreakDurationChange(longBreakDuration: Duration)

        fun onLongBreaksEnabled(longBreaksAreEnabled: Boolean)

        fun onLongBreakFrequencyChange(numberOfSessionsBetweenLongBreaks: Int)

        fun onAutoSessionSwitchEnabled(autoSessionSwitchIsEnabled: Boolean)
    }
}