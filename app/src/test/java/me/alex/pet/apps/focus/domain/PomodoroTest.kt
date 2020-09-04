package me.alex.pet.apps.focus.domain

import me.alex.pet.apps.focus.common.extensions.minutes
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class PomodoroTest {

    private val clock = StubClock()

    private val timer = SessionTimer(clock, 1.minutes)

    private val settingsProvider = StubSettingsProvider()

    private val pomodoro = Pomodoro(timer, settingsProvider)

    @Test
    fun `pomodoro has correct initial state`() {
        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                completedWorkSessionCount = 0,
                remainingDuration = 25.minutes,
                passedDuration = 0.minutes,
                timerState = Timer.State.READY
        )
    }

    @Test
    fun `pomodoro can be started`() {
        pomodoro.startSession()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                completedWorkSessionCount = 0,
                remainingDuration = 25.minutes,
                passedDuration = 0.minutes,
                timerState = Timer.State.RUNNING
        )
    }

    @Test
    fun `running pomodoro can't be started`() {
        pomodoro.startSession()

        assertThrows(IllegalStateException::class.java) {
            pomodoro.startSession()
        }
    }

    @Test
    fun `paused pomodoro can't be started`() {
        pomodoro.startSession()
        pomodoro.pauseSession()

        assertThrows(IllegalStateException::class.java) {
            pomodoro.startSession()
        }
    }

    @Test
    fun `running pomodoro can be paused`() {
        pomodoro.startSession()

        pomodoro.pauseSession()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                timerState = Timer.State.PAUSED
        )
    }

    @Test
    fun `pomodoro can't be paused if it's not running`() {
        assertThrows(IllegalStateException::class.java) {
            pomodoro.pauseSession()
        }

        pomodoro.startSession()
        pomodoro.pauseSession()

        assertThrows(IllegalStateException::class.java) {
            pomodoro.pauseSession()
        }
    }

    @Test
    fun `pomodoro can be toggled`() {
        pomodoro.toggleSession()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                timerState = Timer.State.RUNNING
        )

        pomodoro.toggleSession()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                timerState = Timer.State.PAUSED
        )

        pomodoro.toggleSession()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                timerState = Timer.State.RUNNING
        )
    }

    @Test
    fun `pomodoro can be reset`() {
        // Complete one work session
        pomodoro.startSession()
        clock.atMinute(25L)
        clock.simulateTick()
        pomodoro.startNextSession()

        pomodoro.reset()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                completedWorkSessionCount = 0,
                remainingDuration = 25.minutes,
                passedDuration = 0.minutes,
                timerState = Timer.State.READY
        )
    }

    @Test
    fun `pomodoro tracks the number of completed work sessions`() {
        pomodoro.startSession()

        // Complete the first work session
        clock.atMinute(25L)
        clock.simulateTick()

        assertThat(pomodoro).hasFollowingState(
                completedWorkSessionCount = 1
        )

        // Complete the short break
        pomodoro.startNextSession()
        clock.atMinute(5L)
        clock.simulateTick()

        assertThat(pomodoro).hasFollowingState(
                completedWorkSessionCount = 1
        )

        // Complete one more work session
        pomodoro.startNextSession()
        clock.atMinute(55L)
        clock.simulateTick()

        assertThat(pomodoro).hasFollowingState(
                completedWorkSessionCount = 2
        )

        // Complete one more short break
        pomodoro.startNextSession()
        clock.atMinute(60L)
        clock.simulateTick()

        assertThat(pomodoro).hasFollowingState(
                completedWorkSessionCount = 2
        )
    }

    @Test
    fun `pomodoro doesn't switch sessions without a manual command`() {
        pomodoro.startSession()

        // Complete one session
        clock.atMinute(25L)
        clock.simulateTick()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                timerState = Timer.State.FINISHED
        )
    }

    @Test
    fun `pomodoro switches sessions when a manual command arrives`() {
        // Prepare pomodoro with one completed work session
        pomodoro.startSession()
        clock.atMinute(25L)
        clock.simulateTick()

        pomodoro.startNextSession()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.SHORT_BREAK,
                timerState = Timer.State.RUNNING
        )
    }

    @Test
    fun `pomodoro maintains correct session order`() {
        pomodoro.startSession()

        // 3 pomodoros with short breaks
        for (i in 0..2) {
            assertThat(pomodoro).hasFollowingState(
                    sessionType = SessionType.WORK,
                    remainingDuration = 25.minutes,
                    passedDuration = 0.minutes
            )

            // Complete the work session
            clock.atMinute((i + 1) * 25L)
            clock.simulateTick()
            pomodoro.startNextSession()

            assertThat(pomodoro).hasFollowingState(
                    sessionType = SessionType.SHORT_BREAK,
                    remainingDuration = 5.minutes,
                    passedDuration = 0.minutes
            )

            // Complete the break
            clock.atMinute((i + 1) * 30L)
            clock.simulateTick()
            pomodoro.startNextSession()
        }

        // 4th pomodoro with a long break
        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.WORK,
                remainingDuration = 25.minutes,
                passedDuration = 0.minutes
        )

        // Complete the work session
        clock.atMinute(115L)
        clock.simulateTick()
        pomodoro.startNextSession()

        assertThat(pomodoro).hasFollowingState(
                sessionType = SessionType.LONG_BREAK,
                remainingDuration = 15.minutes,
                passedDuration = 0.minutes
        )
    }

    @Test
    fun `updated settings are applied immediately when the current session is not running`() {
        assertThat(pomodoro).hasFollowingState(
                remainingDuration = 25.minutes,
                passedDuration = 0.minutes,
                timerState = Timer.State.READY
        )

        // Update the settings
        settingsProvider.workDuration = 30.minutes
        settingsProvider.notifyListeners()

        assertThat(pomodoro).hasFollowingState(
                remainingDuration = 30.minutes,
                passedDuration = 0.minutes,
                timerState = Timer.State.READY
        )
    }

    @Test
    fun `updated settings are applied to a next session when the current session is running`() {
        pomodoro.startSession()

        // Update the settings
        settingsProvider.shortBreakDuration = 10.minutes
        settingsProvider.notifyListeners()

        assertThat(pomodoro).hasFollowingState(
                remainingDuration = 25.minutes,
                passedDuration = 0.minutes,
                timerState = Timer.State.RUNNING
        )

        // Complete the session
        clock.atMinute(25L)
        clock.simulateTick()
        pomodoro.startNextSession()

        assertThat(pomodoro).hasFollowingState(
                remainingDuration = 10.minutes,
                passedDuration = 0.minutes,
                timerState = Timer.State.RUNNING
        )
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

    private class StubSettingsProvider : PomodoroSettings {

        override var workDuration: Duration = 25.minutes

        override var shortBreakDuration: Duration = 5.minutes

        override var longBreakDuration: Duration = 15.minutes

        override var longBreaksAreEnabled: Boolean = true

        override var numberOfSessionsBetweenLongBreaks: Int = 3

        private var observers = mutableListOf<PomodoroSettings.Observer>()

        override fun addObserver(observer: PomodoroSettings.Observer) {
            observers = observers.toMutableList().apply {
                add(observer)
            }
        }

        override fun removeObserver(observer: PomodoroSettings.Observer) {
            observers = observers.toMutableList().apply {
                remove(observer)
            }
        }

        fun notifyListeners() {
            observers.forEach { it.onSettingsChange() }
        }
    }
}


private fun AbstractObjectAssert<*, out Pomodoro>.hasFollowingState(
        sessionType: SessionType? = null,
        completedWorkSessionCount: Int? = null,
        remainingDuration: Duration? = null,
        passedDuration: Duration? = null,
        timerState: Timer.State? = null
) {
    if (sessionType != null) extracting { it.sessionType }.isEqualTo(sessionType)
    if (completedWorkSessionCount != null) extracting { it.completedWorkSessionCount }.isEqualTo(completedWorkSessionCount)
    if (remainingDuration != null) extracting { it.remainingDuration }.isEqualTo(remainingDuration)
    if (passedDuration != null) extracting { it.passedDuration }.isEqualTo(passedDuration)
    if (timerState != null) extracting { it.timerState }.isEqualTo(timerState)
}