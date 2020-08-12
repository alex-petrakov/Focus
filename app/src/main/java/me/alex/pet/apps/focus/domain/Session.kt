package me.alex.pet.apps.focus.domain

import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class Session(
        private val clock: Clock,
        val type: SessionType,
        durationSeconds: Long
) {

    private val durationMillis: Long = SECONDS.toMillis(durationSeconds)

    private val clockObserver = object : Clock.Observer {
        override fun onTick(elapsedRealtime: Long) {
            if (timerState != TimerState.RUNNING) {
                return
            }
            val leftDuration = stopTimeInFuture - elapsedRealtime
            remainingMillis = leftDuration
            if (leftDuration <= 0) {
                timerState = TimerState.FINISHED
                clock.stop()
                clock.removeObserver(this)
                observers.forEach { it.onFinish() }
            } else {
                observers.forEach { it.onTick() }
            }
        }
    }

    private var stopTimeInFuture: Long = 0

    private var remainingMillis: Long = durationMillis

    var timerState: TimerState = TimerState.READY
        private set

    val remainingSeconds: Long
        get() = MILLISECONDS.toSeconds(remainingMillis)

    val passedSeconds: Long
        get() = MILLISECONDS.toSeconds(durationMillis - remainingMillis)

    private var observers = mutableListOf<Observer>()

    interface Observer {
        fun onStart()
        fun onResume()
        fun onPause()
        fun onTick()
        fun onFinish()
        fun onCancel()
    }

    init {
        require(durationSeconds > 0) { "Session duration can't be negative" }
    }

    fun start(): Session {
        check(timerState == TimerState.READY) { "A session can be started only once" }
        timerState = TimerState.RUNNING
        observers.forEach { observer ->
            observer.onStart()
            observer.onResume()
        }
        stopTimeInFuture = clock.now() + durationMillis
        clock.addObserver(clockObserver)
        clock.start()
        return this
    }

    fun pause() {
        if (timerState != TimerState.RUNNING) {
            return
        }
        timerState = TimerState.PAUSED
        clock.stop()
        observers.forEach { it.onPause() }
    }

    fun resume() {
        if (timerState != TimerState.PAUSED) {
            return
        }
        timerState = TimerState.RUNNING
        observers.forEach { it.onResume() }
        stopTimeInFuture = clock.now() + remainingMillis
        clock.start()
    }

    fun cancel() {
        timerState = TimerState.CANCELLED
        clock.stop()
        clock.removeObserver(clockObserver)
        observers.forEach { it.onCancel() }
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
}