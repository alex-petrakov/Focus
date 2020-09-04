package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.common.extensions.minutes
import me.alex.pet.apps.focus.domain.SessionType.*
import me.alex.pet.apps.focus.domain.Timer.State
import java.time.Duration

class Pomodoro constructor(
        private val timer: Timer,
        private val settings: PomodoroSettings
) {
    var completedWorkSessionCount = 0
        private set

    private var currentSessionType: SessionType = WORK

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
            return settings.longBreaksAreEnabled &&
                    (completedWorkSessionCount % (settings.numberOfSessionsBetweenLongBreaks + 1) == 0)
        }

    private val timerObserver = object : Timer.Observer {
        override fun onTimerUpdate(event: Timer.Event) {
            when (event) {
                Timer.Event.FINISHED -> onTimerFinished()
                else -> onTimerUpdated()
            }
        }
    }

    private val settingsObserver = object : PomodoroSettings.Observer {
        override fun onSettingsChange() {
            if (timer.state == State.READY) {
                changeSession(currentSessionType)
            }
        }
    }


    init {
        // TODO: unregister the observer
        settings.addObserver(settingsObserver)
        timer.reset(settings.workDuration)
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
        val sessionDuration = when (nextSessionType) {
            WORK -> settings.workDuration
            SHORT_BREAK -> settings.shortBreakDuration
            LONG_BREAK -> settings.longBreakDuration
        }
        currentSessionType = nextSessionType
        timer.reset(sessionDuration)

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

    companion object {
        val defaultWorkDuration = 25.minutes
        val defaultShortBreakDuration = 5.minutes
        val defaultLongBreakDuration = 15.minutes
        const val defaultLongBreakFrequency = 3
        const val defaultLongBreaksAreEnabled = true
    }
}