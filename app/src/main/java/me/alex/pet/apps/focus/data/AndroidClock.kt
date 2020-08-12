package me.alex.pet.apps.focus.data

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import me.alex.pet.apps.focus.domain.Clock
import java.util.concurrent.TimeUnit

class AndroidClock : Clock {

    private val millisBetweenTicks = TimeUnit.SECONDS.toMillis(1L)

    private var observers = mutableListOf<Clock.Observer>()

    private var isRunning = false

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            synchronized(this@AndroidClock) {
                if (!isRunning) {
                    return
                }
                val time = now()
                observers.forEach { it.onTick(time) }
                sendMessageDelayed(obtainMessage(), millisBetweenTicks)
            }
        }
    }


    override fun now(): Long = SystemClock.elapsedRealtime()

    @Synchronized
    override fun start() {
        if (isRunning) {
            return
        }
        handler.sendMessage(handler.obtainMessage(HANDLER_MSG))
        isRunning = true
    }

    @Synchronized
    override fun stop() {
        if (!isRunning) {
            return
        }
        handler.removeMessages(HANDLER_MSG)
        isRunning = false
    }

    @Synchronized
    override fun addObserver(observer: Clock.Observer) {
        observers = observers.toMutableList().apply {
            add(observer)
        }
    }

    @Synchronized
    override fun removeObserver(observer: Clock.Observer) {
        observers = observers.toMutableList().apply {
            remove(observer)
        }
    }
}

private const val HANDLER_MSG = 1