package me.alex.pet.apps.focus.common.extensions

import java.time.Duration

val Duration.isPositive: Boolean get() = !(isNegative || isZero)

val Number.milliseconds: Duration get() = Duration.ofMillis(this.toLong())

val Number.seconds: Duration get() = Duration.ofSeconds(this.toLong())

val Number.minutes: Duration get() = Duration.ofMinutes(this.toLong())

