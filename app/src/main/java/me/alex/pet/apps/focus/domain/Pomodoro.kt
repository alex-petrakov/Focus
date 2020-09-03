package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.domain.SessionType.*
import me.alex.pet.apps.focus.domain.Timer.State
import java.time.Duration

class Pomodoro constructor(
        private val timer: Timer,
        settingsRepository: PomodoroSettingsRepository
) {
    var completedWorkSessionCount = 0
        private set

    private var currentSessionType: SessionType = WORK

    private var currentSettings: Settings = settingsRepository.settings

    private var updatedSettings: Settings = currentSettings

    private var observers = mutableListOf<Observer>()

    interface Observer {
        fun onUpdate()
    }

    val remainingDuration: Duration
        get() = timer.remainingDuration

    val passedDuration: Duration
        get() = timer.passedDuration

    val progress: Double
        get() = timer.progress

    val sessionType: SessionType
        get() = currentSessionType

    val timerState: State
        get() = timer.state

    val isAwaitingSessionSwitch
        get() = timerState == State.FINISHED && remainingDuration.isZero

    val nextSessionType
        get() = when (currentSessionType) {
            WORK -> if (nextSessionIsLongBreak) LONG_BREAK else SHORT_BREAK
            SHORT_BREAK, LONG_BREAK -> WORK
        }

    private val nextSessionIsLongBreak: Boolean
        get() {
            return currentSettings.longBreaksAreEnabled &&
                    (completedWorkSessionCount % (currentSettings.numberOfSessionsBetweenLongBreaks + 1) == 0)
        }

    private val timerObserver = object : Timer.Observer {
        override fun onTimerUpdate(event: Timer.Event) {
            when (event) {
                Timer.Event.FINISHED -> onTimerFinished()
                else -> onTimerUpdated()
            }
        }
    }

    private val settingsObserver = object : PomodoroSettingsRepository.Observer {
        override fun onSettingsChange(settings: Settings) {
            updatedSettings = settings
            if (timer.state == State.READY) {
                changeSession(currentSessionType)
            }
        }
    }


    init {
        // TODO: unregister the observer
        settingsRepository.addObserver(settingsObserver)
        timer.reset(duration = currentSettings.workDuration)
        timer.addObserver(timerObserver)
        currentSessionType = WORK
    }

    fun startSession() {
        check(timer.state == State.READY) { "Only a session in the '${State.READY}' state can be started" }
        timer.start()
    }

    fun pauseSession() {
        check(timer.state == State.RUNNING) { "Only a session in the '${State.RUNNING}' state can be paused" }
        timer.pause()
    }

    fun resumeSession() {
        check(timer.state == State.PAUSED) { "Only a session in the '${State.PAUSED}' state can be resumed" }
        timer.resume()
    }

    fun toggleSession() {
        when (timer.state) {
            State.READY -> timer.start()
            State.RUNNING -> timer.pause()
            State.PAUSED -> timer.resume()
            else -> throw IllegalStateException("Finished or cancelled session can't be toggled")
        }
    }

    fun startNextSession() {
        check(isAwaitingSessionSwitch) { "currentSession isn't completed yet" }
        changeSession(nextSessionType)
        startSession()
    }

    private fun changeSession(nextSessionType: SessionType) {
        currentSettings = updatedSettings

        val sessionDuration = when (nextSessionType) {
            WORK -> currentSettings.workDuration
            SHORT_BREAK -> currentSettings.shortBreakDuration
            LONG_BREAK -> currentSettings.longBreakDuration
        }
        currentSessionType = nextSessionType
        timer.reset(duration = sessionDuration)

        notifyObservers()
    }

    fun reset() {
        timer.cancel()
        completedWorkSessionCount = 0
        changeSession(WORK)
    }

    fun addObserver(observer: Observer) {
        observers = observers.toMutableList().apply {
            add(observer)
        }
    }

    fun removeObserver(observer: Observer) {
        observers = observers.toMutableList().apply {
            remove(observer)
        }
    }

    private fun onTimerFinished() {
        if (currentSessionType == WORK) {
            completedWorkSessionCount++
        }
        notifyObservers()
    }

    private fun onTimerUpdated() {
        notifyObservers()
    }

    private fun notifyObservers() {
        observers.forEach { it.onUpdate() }
    }


    data class Settings(
            val workDuration: Duration,
            val shortBreakDuration: Duration,
            val longBreakDuration: Duration,
            val longBreaksAreEnabled: Boolean,
            val numberOfSessionsBetweenLongBreaks: Int
    )

    companion object {
        const val DEFAULT_WORK_DURATION_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_DURATION = 5
        const val DEFAULT_LONG_BREAK_DURATION = 15
        const val DEFAULT_LONG_BREAK_FREQUENCY = 4
    }
}