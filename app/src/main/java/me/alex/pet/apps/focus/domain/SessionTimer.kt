package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.common.extensions.isPositive
import me.alex.pet.apps.focus.domain.Timer.State
import java.time.Duration

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
            val leftDuration = stopTimeInFuture - elapsedRealtime
            remainingMillis = leftDuration
            if (leftDuration <= 0) {
                state = State.FINISHED
                clock.stop()
                clock.removeObserver(this)
                observers.forEach { it.onFinish() }
            } else {
                observers.forEach { it.onTick() }
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
        require(duration.isPositive) { "Timer duration can't be negative" }
    }

    override fun start() {
        check(state == State.READY) { "This timer is not in a ${State.READY} state" }
        state = State.RUNNING
        observers.forEach { observer ->
            observer.onStart()
            observer.onResume()
        }
        stopTimeInFuture = clock.now() + durationMillis
        clock.addObserver(clockObserver)
        clock.start()
    }

    override fun pause() {
        if (state != State.RUNNING) {
            return
        }
        state = State.PAUSED
        clock.stop()
        observers.forEach { it.onPause() }
    }

    override fun resume() {
        if (state != State.PAUSED) {
            return
        }
        state = State.RUNNING
        observers.forEach { it.onResume() }
        stopTimeInFuture = clock.now() + remainingMillis
        clock.start()
    }

    override fun cancel() {
        state = State.CANCELLED
        clock.stop()
        clock.removeObserver(clockObserver)
        observers.forEach { it.onCancel() }
    }

    override fun reset(duration: Duration) {
        state = State.READY
        durationMillis = duration.toMillis()
        remainingMillis = durationMillis
        clock.stop()
        clock.removeObserver(clockObserver)
        observers.forEach { it.onReset() }
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