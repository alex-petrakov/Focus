package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.common.extensions.minutes
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.TimeUnit

class SessionTest {

    private val clock: StubClock = StubClock()

    @Test
    fun `session has correct initial state`() {
        val session = createSession()

        assertThat(session).hasState(timerState = TimerState.READY, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    fun `sessions with negative duration are not allowed`() {
        assertThrows<IllegalArgumentException> { createSession(duration = (-1).minutes) }
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `session can be started`() {
        val session = createSession()

        session.start()

        assertThat(clock.isRunning).isTrue()
        assertThat(session).hasState(timerState = TimerState.RUNNING, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    fun `session can't be started twice`() {
        val session = createSession().apply {
            start()
        }

        assertThrows<IllegalStateException> { session.start() }
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `session can be paused`() {
        val session = createSession().apply {
            start()
        }

        session.pause()

        assertThat(clock.isRunning).isFalse()
        assertThat(session).hasState(timerState = TimerState.PAUSED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `session can be resumed`() {
        val session = createSession().apply {
            start()
            pause()
        }

        session.resume()

        assertThat(clock.isRunning).isTrue()
        assertThat(session).hasState(timerState = TimerState.RUNNING, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `fresh session can be cancelled`() {
        val session = createSession()

        session.cancel()

        assertThat(clock.isRunning).isFalse()
        assertThat(session).hasState(timerState = TimerState.CANCELLED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `running session can be cancelled`() {
        val session = createSession().apply {
            start()
        }

        session.cancel()

        assertThat(clock.isRunning).isFalse()
        assertThat(session).hasState(timerState = TimerState.CANCELLED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `paused session can be cancelled`() {
        val session = createSession().apply {
            start()
            pause()
        }

        session.cancel()

        assertThat(clock.isRunning).isFalse()
        assertThat(session).hasState(timerState = TimerState.CANCELLED, remainingDuration = 10.minutes, passedDuration = 0.minutes)
    }

    @Test
    fun `session updates progress on each timer tick`() {
        val session = createSession().apply {
            start()
        }

        assertThat(session).hasState(timerState = TimerState.RUNNING, remainingDuration = 10.minutes, passedDuration = 0.minutes)

        clock.atMinute(1L)
        clock.simulateTick()
        assertThat(session).hasState(timerState = TimerState.RUNNING, remainingDuration = 9.minutes, passedDuration = 1.minutes)

        clock.atMinute(2L)
        clock.simulateTick()
        assertThat(session).hasState(timerState = TimerState.RUNNING, remainingDuration = 8.minutes, passedDuration = 2.minutes)

        clock.atMinute(5L)
        clock.simulateTick()
        assertThat(session).hasState(timerState = TimerState.RUNNING, remainingDuration = 5.minutes, passedDuration = 5.minutes)
    }

    @Test
    @Suppress("UsePropertyAccessSyntax")
    fun `session ends when time is up`() {
        val session = createSession().apply {
            start()
        }

        clock.atMinute(10L)
        clock.simulateTick()

        assertThat(clock.isRunning).isFalse()
        assertThat(session).hasState(timerState = TimerState.FINISHED, remainingDuration = 0.minutes, passedDuration = 10.minutes)
    }

    private fun createSession(
            clock: Clock = this.clock,
            sessionType: SessionType = SessionType.WORK,
            duration: Duration = 10.minutes
    ): Session {
        return Session(clock, sessionType, duration)
    }

    class StubClock : Clock {

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

private fun AbstractObjectAssert<*, Session>.hasState(timerState: TimerState, remainingDuration: Duration, passedDuration: Duration) {
    extracting { session -> session.timerState }.isEqualTo(timerState)
    extracting { it.remainingDuration }.isEqualTo(remainingDuration)
    extracting { it.passedDuration }.isEqualTo(passedDuration)
}