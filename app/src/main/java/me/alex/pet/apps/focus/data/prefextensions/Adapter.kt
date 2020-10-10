package me.alex.pet.apps.focus.data.prefextensions

interface Adapter<T, P> {
    fun toPref(value: T): P

    fun fromPref(prefValue: P): T
}