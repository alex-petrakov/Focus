package me.alex.pet.apps.focus.domain

interface Clock {

    fun now(): Long

    fun start()

    fun stop()

    fun addObserver(observer: Observer)

    fun removeObserver(observer: Observer)

    interface Observer {
        fun onTick(elapsedRealtime: Long)
    }
}