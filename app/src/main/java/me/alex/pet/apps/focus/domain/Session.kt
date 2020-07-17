package me.alex.pet.apps.focus.domain

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class Session(
        val type: SessionType,
        durationSeconds: Long
) {

    private val durationMillis: Long = SECONDS.toMillis(durationSeconds)

    private val countdownIntervalMillis: Long = SECONDS.toMillis(1)

    private var stopTimeInFuture: Long = 0

    private var remainingMillis: Long = durationMillis

    var timerState: TimerState = TimerState.READY
        @Synchronized get
        private set

    val remainingSeconds: Long
        @Synchronized get() = MILLISECONDS.toSeconds(remainingMillis)

    val passedSeconds: Long
        @Synchronized get() = MILLISECONDS.toSeconds(durationMillis - remainingMillis)

    private val observers = mutableListOf<Observer>()

    interface Observer {
        fun onStart()
        fun onResume()
        fun onPause()
        fun onTick()
        fun onFinish()
        fun onCancel()
    }

    @Synchronized
    fun start(): Session {
        check(timerState == TimerState.READY) { "A session can be started only once." }
        timerState = TimerState.RUNNING
        if (durationMillis <= 0) {
            timerState = TimerState.FINISHED
            observers.forEach { it.onFinish() }
            return this
        }
        observers.forEach { observer ->
            observer.onStart()
            observer.onResume()
        }
        stopTimeInFuture = SystemClock.elapsedRealtime() + durationMillis
        handler.sendMessage(handler.obtainMessage(HANDLER_MSG))
        return this
    }

    @Synchronized
    fun pause() {
        if (timerState != TimerState.RUNNING) {
            return
        }
        timerState = TimerState.PAUSED
        handler.removeMessages(HANDLER_MSG)
        observers.forEach { it.onPause() }
    }

    @Synchronized
    fun resume() {
        if (timerState != TimerState.PAUSED) {
            return
        }
        timerState = TimerState.RUNNING
        observers.forEach { it.onResume() }
        stopTimeInFuture = SystemClock.elapsedRealtime() + remainingMillis
        handler.sendMessage(handler.obtainMessage(HANDLER_MSG))
    }

    @Synchronized
    fun cancel() {
        timerState = TimerState.CANCELLED
        handler.removeMessages(HANDLER_MSG)
        observers.forEach { it.onCancel() }
    }

    @Synchronized
    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    @Synchronized
    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            synchronized(this@Session) {
                if (timerState != TimerState.RUNNING) {
                    return
                }
                val millisLeft = stopTimeInFuture - SystemClock.elapsedRealtime()
                remainingMillis = millisLeft
                if (millisLeft <= 0) {
                    timerState = TimerState.FINISHED
                    observers.forEach { it.onFinish() }
                } else {
                    val lastTickStart = SystemClock.elapsedRealtime()
                    observers.forEach { it.onTick() }
                    // Take into account user's onTick taking time to execute
                    val lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart
                    var delay: Long
                    if (millisLeft < countdownIntervalMillis) {
                        // Just delay until done
                        delay = millisLeft - lastTickDuration
                        // Special case: user's onTick took more than interval to
                        // Complete, trigger onFinish without delay
                        if (delay < 0) delay = 0
                    } else {
                        delay = countdownIntervalMillis - lastTickDuration
                        // Special case: user's onTick took more than interval to
                        // Complete, skip to next interval
                        while (delay < 0) delay += countdownIntervalMillis
                    }
                    sendMessageDelayed(obtainMessage(HANDLER_MSG), delay)
                }
            }
        }
    }
}

private const val HANDLER_MSG = 1