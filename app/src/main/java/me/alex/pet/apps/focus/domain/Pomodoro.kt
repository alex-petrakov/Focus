package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.domain.SessionType.*

class Pomodoro constructor(
        private val configurationRepository: PomodoroConfigurationRepository
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

            if (autoSwitchBetweenSessions) {
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

    private var session: Session = Session(WORK, workDuration.toLong()).apply {
        addObserver(sessionObserver)
    }

    private val pomodoroConfigurationObserver = object : PomodoroConfigurationRepository.PomodoroConfigurationObserver {
        override fun onWorkDurationChange(workDurationMins: Int) {
            // TODO: consider adding a check against invalid values
            if (session.timerState == TimerState.READY && session.type == WORK) {
                changeSession(WORK)
            }
        }

        override fun onShortBreakDurationChange(shortBreakDurationMins: Int) {
            if (session.timerState == TimerState.READY && session.type == SHORT_BREAK) {
                changeSession(SHORT_BREAK)
            }
        }

        override fun onLongBreakDurationChange(longBreakDurationMins: Int) {
            if (session.timerState == TimerState.READY && session.type == LONG_BREAK) {
                changeSession(LONG_BREAK)
            }
        }

        override fun onLongBreaksEnabled(longBreaksAreEnabled: Boolean) {
            // Do nothing
        }

        override fun onLongBreakFrequencyChange(numberOfSessionsBetweenLongBreaks: Int) {
            // Do nothing
        }

        override fun onAutoSessionSwitchEnabled(autoSessionSwitchIsEnabled: Boolean) {
            // Do nothing
        }
    }

    var completedWorkSessionCount = 0
        private set

    var isAwaitingSessionSwitch = false
        private set

    private val observers = mutableListOf<Observer>()

    interface Observer {
        fun onUpdate()
    }

    val remainingSeconds: Long
        get() = session.remainingSeconds

    val passedSeconds: Long
        get() = session.passedSeconds

    val sessionType: SessionType
        get() = session.type

    val timerState: TimerState
        get() = session.timerState

    val nextSessionType
        get() = when (session.type) {
            WORK -> if (nextSessionIsLongBreak) LONG_BREAK else SHORT_BREAK
            SHORT_BREAK, LONG_BREAK -> WORK
        }

    private val nextSessionIsLongBreak
        get() = longBreaksAreEnabled && (completedWorkSessionCount % (numberOfSessionsBetweenLongBreaks + 1) == 0)

    val workDuration: Seconds
        get() = configurationRepository.workDurationMins

    val shortBreakDuration: Seconds
        get() = configurationRepository.shortBreakDurationMins

    val longBreakDuration: Seconds
        get() = configurationRepository.longBreakDurationMins

    val longBreaksAreEnabled: Boolean
        get() = configurationRepository.longBreaksAreEnabled

    val numberOfSessionsBetweenLongBreaks: Int
        get() = configurationRepository.numberOfSessionsBetweenLongBreaks

    val autoSwitchBetweenSessions: Boolean
        get() = configurationRepository.autoSessionSwitchIsEnabled

    init {
        // TODO: unregister the observer
        configurationRepository.addPomodoroConfigurationObserver(pomodoroConfigurationObserver)
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

    private fun changeSession(nextSessionType: SessionType) {
        val nextSession = when (nextSessionType) {
            WORK -> Session(WORK, workDuration.toLong())
            SHORT_BREAK -> Session(SHORT_BREAK, shortBreakDuration.toLong())
            LONG_BREAK -> Session(LONG_BREAK, longBreakDuration.toLong())
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
        observers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    private fun notifyObservers() {
        observers.forEach { it.onUpdate() }
    }

    companion object {
        const val DEFAULT_WORK_DURATION_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_DURATION = 5
        const val DEFAULT_LONG_BREAK_DURATION = 15
        const val DEFAULT_LONG_BREAK_FREQUENCY = 4
    }
}


typealias Seconds = Int