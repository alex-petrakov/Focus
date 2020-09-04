package me.alex.pet.apps.focus.domain

import java.time.Duration

interface Timer {

    val remainingDuration: Duration

    val passedDuration: Duration

    val progress: Double

    val state: State

    fun start()

    fun pause()

    fun resume()

    fun cancel()

    fun reset(duration: Duration)

    fun addObserver(observer: Observer)

    fun removeObserver(observer: Observer)


    interface Observer {
        fun onTimerUpdate(event: Event)
    }

    enum class State {
        READY,
        RUNNING,
        PAUSED,
        FINISHED,
        CANCELLED
    }

    enum class Event {
        STARTED,
        RESUMED,
        PAUSED,
        TICK,
        FINISHED,
        CANCELLED,
        RESET
    }
}