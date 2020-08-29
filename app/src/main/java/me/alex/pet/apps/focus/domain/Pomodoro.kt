package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.domain.SessionType.*
import me.alex.pet.apps.focus.domain.Timer.State
import java.time.Duration

class Pomodoro constructor(
        private val timer: Timer,
        settingsRepository: PomodoroSettingsRepository
) {
    private val timerObserver = object : Timer.Observer {
        override fun onStart() {
            notifyObservers()
        }

        override fun onResume() {
            notifyObservers()
        }

        override fun onPause() {
            notifyObservers()
        }

        override fun onTick() {
            notifyObservers()
        }

        override fun onFinish() {
            if (currentSessionType == WORK) {
                completedWorkSessionCount++
            }
            notifyObservers()

            if (currentSettings.autoSessionSwitchIsEnabled) {
                changeSession(nextSessionType)
                startSession()
            } else {
                isAwaitingSessionSwitch = true
                notifyObservers()
            }
        }

        override fun onCancel() {
            notifyObservers()
        }

        override fun onReset() {
            notifyObservers()
        }
    }

    private var currentSettings: Settings = settingsRepository.settings

    private var updatedSettings: Settings = currentSettings

    private var currentSessionType: SessionType = WORK

    private val settingsObserver = object : PomodoroSettingsRepository.Observer {
        override fun onSettingsChange(settings: Settings) {
            updatedSettings = settings
            if (timer.state == State.READY) {
                changeSession(currentSessionType)
            }
        }
    }

    var completedWorkSessionCount = 0
        private set

    var isAwaitingSessionSwitch = false
        private set

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
        isAwaitingSessionSwitch = false
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
        isAwaitingSessionSwitch = false
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

    private fun notifyObservers() {
        observers.forEach { it.onUpdate() }
    }


    data class Settings(
            val workDuration: Duration,
            val shortBreakDuration: Duration,
            val longBreakDuration: Duration,
            val longBreaksAreEnabled: Boolean,
            val numberOfSessionsBetweenLongBreaks: Int,
            val autoSessionSwitchIsEnabled: Boolean
    )

    companion object {
        const val DEFAULT_WORK_DURATION_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_DURATION = 5
        const val DEFAULT_LONG_BREAK_DURATION = 15
        const val DEFAULT_LONG_BREAK_FREQUENCY = 4
    }
}