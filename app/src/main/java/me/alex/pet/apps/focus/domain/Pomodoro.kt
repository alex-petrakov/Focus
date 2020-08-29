package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.domain.Session.TimerState
import me.alex.pet.apps.focus.domain.Session.Type
import me.alex.pet.apps.focus.domain.Session.Type.*
import java.time.Duration

class Pomodoro constructor(
        private val clock: Clock,
        settingsRepository: PomodoroSettingsRepository
) {
    private val sessionObserver = object : Session.Observer {
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
            if (session.type == WORK) {
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
    }

    private var currentSettings: Settings = settingsRepository.settings

    private var updatedSettings: Settings = currentSettings

    private var session: Session = Session(clock, WORK, currentSettings.workDuration).apply {
        addObserver(sessionObserver)
    }

    private val settingsObserver = object : PomodoroSettingsRepository.Observer {
        override fun onSettingsChange(settings: Settings) {
            updatedSettings = settings
            if (session.timerState == TimerState.READY) {
                changeSession(session.type)
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
        get() = session.remainingDuration

    val passedDuration: Duration
        get() = session.passedDuration

    val progress: Double
        get() = session.progress

    val sessionType: Type
        get() = session.type

    val timerState: TimerState
        get() = session.timerState

    val nextSessionType
        get() = when (session.type) {
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
    }

    fun startSession() {
        check(session.timerState == TimerState.READY) { "Only a session in the '${TimerState.READY}' state can be started" }
        session.start()
    }

    fun pauseSession() {
        check(session.timerState == TimerState.RUNNING) { "Only a session in the '${TimerState.RUNNING}' state can be paused" }
        session.pause()
    }

    fun resumeSession() {
        check(session.timerState == TimerState.PAUSED) { "Only a session in the '${TimerState.PAUSED}' state can be resumed" }
        session.resume()
    }

    fun toggleSession() {
        when (session.timerState) {
            TimerState.READY -> session.start()
            TimerState.RUNNING -> session.pause()
            TimerState.PAUSED -> session.resume()
            else -> throw IllegalStateException("Finished or cancelled session can't be toggled")
        }
    }

    fun startNextSession() {
        check(isAwaitingSessionSwitch) { "currentSession isn't completed yet" }
        isAwaitingSessionSwitch = false
        changeSession(nextSessionType)
        startSession()
    }

    private fun changeSession(nextSessionType: Type) {
        currentSettings = updatedSettings
        val nextSession = when (nextSessionType) {
            WORK -> Session(clock, WORK, currentSettings.workDuration)
            SHORT_BREAK -> Session(clock, SHORT_BREAK, currentSettings.shortBreakDuration)
            LONG_BREAK -> Session(clock, LONG_BREAK, currentSettings.longBreakDuration)
        }
        session.removeObserver(sessionObserver)
        session = nextSession.apply {
            addObserver(sessionObserver)
        }
        notifyObservers()
    }

    fun reset() {
        session.cancel()
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