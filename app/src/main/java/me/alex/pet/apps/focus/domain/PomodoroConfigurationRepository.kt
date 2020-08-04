package me.alex.pet.apps.focus.domain

interface PomodoroConfigurationRepository {

    val workDurationMins: Int

    val shortBreakDurationMins: Int

    val longBreakDurationMins: Int

    val longBreaksAreEnabled: Boolean

    val numberOfSessionsBetweenLongBreaks: Int

    val autoSessionSwitchIsEnabled: Boolean

    fun addPomodoroConfigurationObserver(observer: PomodoroConfigurationObserver)

    fun removePomodoroConfigurationObserver(observer: PomodoroConfigurationObserver)

    interface PomodoroConfigurationObserver {
        fun onWorkDurationChange(workDurationMins: Int)

        fun onShortBreakDurationChange(shortBreakDurationMins: Int)

        fun onLongBreakDurationChange(longBreakDurationMins: Int)

        fun onLongBreaksEnabled(longBreaksAreEnabled: Boolean)

        fun onLongBreakFrequencyChange(numberOfSessionsBetweenLongBreaks: Int)

        fun onAutoSessionSwitchEnabled(autoSessionSwitchIsEnabled: Boolean)
    }
}