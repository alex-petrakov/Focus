package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.domain.SessionType.*

class Pomodoro constructor(
        val workDuration: Seconds,
        val shortBreakDuration: Seconds,
        val longBreakDuration: Seconds,
        val longBreaksAreEnabled: Boolean,
        val numberOfSessionsBetweenLongBreaks: Int,
        val autoSwitchBetweenSessions: Boolean
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

    init {
        require(workDuration > 0) { "workDuration must be positive" }
        require(shortBreakDuration > 0) { "shortBreakDuration must be positive" }
        require(longBreakDuration > 0) { "longBreakDuration must be positive" }
        require(numberOfSessionsBetweenLongBreaks > 0) { "numberOfSessionsBetweenLongBreaks must be positive" }
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
}


typealias Seconds = Int