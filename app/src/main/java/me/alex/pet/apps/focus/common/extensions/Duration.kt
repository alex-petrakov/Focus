package me.alex.pet.apps.focus.common.extensions

import java.time.Duration
import java.time.temporal.TemporalUnit

val Duration.isPositive: Boolean get() = !(isNegative || isZero)

fun Duration.toIntMinutes(): Int = this.toMinutes().clampToInt()

fun Duration.toInt(unit: TemporalUnit): Int = this.get(unit).clampToInt()

val Number.milliseconds: Duration get() = Duration.ofMillis(this.toLong())

val Number.seconds: Duration get() = Duration.ofSeconds(this.toLong())

val Number.minutes: Duration get() = Duration.ofMinutes(this.toLong())

private fun Long.clampToInt(): Int {
    return when {
        this > Int.MAX_VALUE -> Int.MAX_VALUE
        this < Int.MIN_VALUE -> Int.MIN_VALUE
        else -> toInt()
    }
}

