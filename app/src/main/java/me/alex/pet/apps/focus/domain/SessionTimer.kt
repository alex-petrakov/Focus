package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.domain.Timer.State
import java.time.Duration
import kotlin.math.max

class SessionTimer(
        private val clock: Clock,
        duration: Duration
) : Timer {

    private var durationMillis: Long = duration.toMillis()

    private var remainingMillis: Long = durationMillis

    private val clockObserver = object : Clock.Observer {
        override fun onTick(elapsedRealtime: Long) {
            if (state != State.RUNNING) {
                return
            }
            val leftDuration = max(stopTimeInFuture - elapsedRealtime, 0L)
            remainingMillis = leftDuration
            if (leftDuration == 0L) {
                state = State.FINISHED
                clock.stop()
                clock.removeObserver(this)
                observers.forEach { it.onTimerUpdate(Timer.Event.FINISHED) }
            } else {
                observers.forEach { it.onTimerUpdate(Timer.Event.TICK) }
            }
        }
    }

    private var stopTimeInFuture: Long = 0

    override var state: State = State.READY
        private set

    override val remainingDuration: Duration
        get() = Duration.ofMillis(remainingMillis)

    override val passedDuration: Duration
        get() = Duration.ofMillis(durationMillis - remainingMillis)

    override val progress: Double
        get() = passedDuration.toMillis() * 100.0 / durationMillis

    private var observers = mutableListOf<Timer.Observer>()

    init {
        require(!duration.isNegative) { "Timer duration can't be negative" }
    }

    override fun start() {
        check(state == State.READY) { "This timer is not in a ${State.READY} state" }
        state = State.RUNNING
        stopTimeInFuture = clock.now() + durationMillis
        observers.forEach { observer ->
            observer.onTimerUpdate(Timer.Event.STARTED)
            observer.onTimerUpdate(Timer.Event.RESUMED)
        }
        if (durationMillis == 0L) {
            observers.forEach { it.onTimerUpdate(Timer.Event.FINISHED) }
            state = State.FINISHED
            return
        }
        clock.addObserver(clockObserver)
        clock.start()
    }

    override fun pause() {
        if (state != State.RUNNING) {
            return
        }
        state = State.PAUSED
        clock.stop()
        observers.forEach { it.onTimerUpdate(Timer.Event.PAUSED) }
    }

    override fun resume() {
        if (state != State.PAUSED) {
            return
        }
        state = State.RUNNING
        observers.forEach { it.onTimerUpdate(Timer.Event.RESUMED) }
        stopTimeInFuture = clock.now() + remainingMillis
        clock.start()
    }

    override fun cancel() {
        state = State.CANCELLED
        clock.stop()
        clock.removeObserver(clockObserver)
        observers.forEach { it.onTimerUpdate(Timer.Event.CANCELLED) }
    }

    override fun reset(duration: Duration) {
        require(!duration.isNegative) { "Timer duration can't be negative" }
        state = State.READY
        durationMillis = duration.toMillis()
        remainingMillis = durationMillis
        clock.stop()
        clock.removeObserver(clockObserver)
        observers.forEach { it.onTimerUpdate(Timer.Event.RESET) }
    }

    override fun addObserver(observer: Timer.Observer) {
        observers = observers.toMutableList().apply {
            add(observer)
        }
    }

    override fun removeObserver(observer: Timer.Observer) {
        observers = observers.toMutableList().apply {
            remove(observer)
        }
    }
}