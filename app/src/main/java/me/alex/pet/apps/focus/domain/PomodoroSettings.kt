package me.alex.pet.apps.focus.domain

import java.time.Duration

interface PomodoroSettings {

    var workDuration: Duration

    var shortBreakDuration: Duration

    var longBreakDuration: Duration

    var longBreaksAreEnabled: Boolean

    var numberOfSessionsBetweenLongBreaks: Int

    fun addObserver(observer: Observer)

    fun removeObserver(observer: Observer)

    interface Observer {
        fun onSettingsChange()
    }
}