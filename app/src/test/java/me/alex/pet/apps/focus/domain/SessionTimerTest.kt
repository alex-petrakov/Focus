package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.common.extensions.minutes
import me.alex.pet.apps.focus.domain.Timer.State
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.TimeUnit

class SessionTimerTest {

    private val clock: StubClock = StubClock()

    @Test
    fun `timer has correct initial state`() {
        val timer = createTimer()

        assertThat(timer).hasState(timerState = State.READY, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    fun `timers with negative duration are not allowed`() {
        assertThrows<IllegalArgumentException> { createTimer(duration = (-1).minutes) }
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `timer can be started`() {
        val timer = createTimer()

        timer.start()

        assertThat(timer).hasState(timerState = State.RUNNING, remainingDuration = 10.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isTrue()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `timer with 0 duration finishes immediately after start`() {
        val timer = createTimer(duration = 0.minutes)

        timer.start()

        assertThat(timer).hasState(timerState = State.FINISHED, remainingDuration = 0.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    fun `timer can't be started twice`() {
        val timer = createTimer().apply {
            start()
        }

        assertThrows<IllegalStateException> { timer.start() }
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `timer can be paused`() {
        val timer = createTimer().apply {
            start()
        }

        timer.pause()

        assertThat(timer).hasState(timerState = State.PAUSED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `timer can be resumed`() {
        val timer = createTimer().apply {
            start()
            pause()
        }

        timer.resume()

        assertThat(timer).hasState(timerState = State.RUNNING, remainingDuration = 10.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isTrue()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `fresh timer can be cancelled`() {
        val timer = createTimer()

        timer.cancel()

        assertThat(timer).hasState(timerState = State.CANCELLED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `running timer can be cancelled`() {
        val timer = createTimer().apply {
            start()
        }

        timer.cancel()

        assertThat(timer).hasState(timerState = State.CANCELLED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `paused timer can be cancelled`() {
        val timer = createTimer().apply {
            start()
            pause()
        }

        timer.cancel()

        assertThat(timer).hasState(timerState = State.CANCELLED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `fresh timer can be reset`() {
        val timer = createTimer()

        timer.reset(duration = 5.minutes)

        assertThat(timer).hasState(timerState = State.READY, remainingDuration = 5.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `running timer can be reset`() {
        val timer = createTimer().apply {
            start()
        }
        clock.atMinute(1L)

        timer.reset(duration = 5.minutes)

        assertThat(timer).hasState(timerState = State.READY, remainingDuration = 5.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `paused timer can be reset`() {
        val timer = createTimer().apply {
            start()
        }
        clock.atMinute(1L)
        timer.pause()

        timer.reset(duration = 5.minutes)

        assertThat(timer).hasState(timerState = State.READY, remainingDuration = 5.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `finished timer can be reset`() {
        val timer = createTimer().apply {
            start()
        }
        clock.atMinute(10L)

        timer.reset(duration = 5.minutes)

        assertThat(timer).hasState(timerState = State.READY, remainingDuration = 5.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `cancelled timer can be reset`() {
        val timer = createTimer().apply {
            start()
        }
        clock.atMinute(1L)
        timer.cancel()

        timer.reset(duration = 5.minutes)

        assertThat(timer).hasState(timerState = State.READY, remainingDuration = 5.minutes, passedDuration = 0.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    @Test
    fun `timer can't be reset with a negative duration`() {
        val timer = createTimer()

        assertThrows<IllegalArgumentException> {
            timer.reset(duration = (-1).minutes)
        }
    }

    @Test
    fun `timer updates progress on each timer tick`() {
        val timer = createTimer().apply {
            start()
        }

        assertThat(timer).hasState(timerState = State.RUNNING, remainingDuration = 10.minutes, passedDuration = 0.minutes)

        clock.atMinute(1L)
        clock.simulateTick()
        assertThat(timer).hasState(timerState = State.RUNNING, remainingDuration = 9.minutes, passedDuration = 1.minutes)

        clock.atMinute(2L)
        clock.simulateTick()
        assertThat(timer).hasState(timerState = State.RUNNING, remainingDuration = 8.minutes, passedDuration = 2.minutes)

        clock.atMinute(5L)
        clock.simulateTick()
        assertThat(timer).hasState(timerState = State.RUNNING, remainingDuration = 5.minutes, passedDuration = 5.minutes)
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `timer transitions into finished state when time is up`() {
        val timer = createTimer().apply {
            start()
        }

        clock.atMinute(10L)
        clock.simulateTick()

        assertThat(timer).hasState(timerState = State.FINISHED, remainingDuration = 0.minutes, passedDuration = 10.minutes)
        assertThat(clock.isRunning).isFalse()
    }

    private fun createTimer(
            clock: Clock = this.clock,
            duration: Duration = 10.minutes
    ): SessionTimer {
        return SessionTimer(clock, duration)
    }

    private class StubClock : Clock {

        private var observers = mutableListOf<Clock.Observer>()

        private var time: Long = 0L

        var isRunning = false

        override fun now(): Long = 0L

        override fun start() {
            isRunning = true
        }

        override fun stop() {
            isRunning = false
        }

        override fun addObserver(observer: Clock.Observer) {
            observers = observers.toMutableList().apply {
                add(observer)
            }
        }

        override fun removeObserver(observer: Clock.Observer) {
            observers = observers.toMutableList().apply {
                remove(observer)
            }
        }

        fun simulateTick() {
            observers.forEach { it.onTick(time) }
        }

        fun atMinute(minute: Long) {
            this.time = TimeUnit.MINUTES.toMillis(minute)
        }
    }
}

private fun AbstractObjectAssert<*, out Timer>.hasState(timerState: State, remainingDuration: Duration, passedDuration: Duration) {
    extracting { it.state }.isEqualTo(timerState)
    extracting { it.remainingDuration }.isEqualTo(remainingDuration)
    extracting { it.passedDuration }.isEqualTo(passedDuration)
}