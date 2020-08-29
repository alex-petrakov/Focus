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
        fun onStart()
        fun onResume()
        fun onPause()
        fun onTick()
        fun onFinish()
        fun onCancel()
        fun onReset()
    }

    enum class State {
        READY,
        RUNNING,
        PAUSED,
        FINISHED,
        CANCELLED
    }
}