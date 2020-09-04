package me.alex.pet.apps.focus.data

import android.content.Context
import android.content.SharedPreferences
import com.chibatching.kotpref.KotprefModel
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.PomodoroSettings
import me.alex.pet.apps.focus.domain.PomodoroSettings.Observer
import timber.log.Timber
import java.time.Duration

class PomodoroPrefs(context: Context) : KotprefModel(context), PomodoroSettings {

    override val kotprefName = context.getString(R.string.prefs_app)

    override var workDuration: Duration
        get() = Duration.ofMinutes(_workDurationInMinutes.toLong())
        set(value) {
            _workDurationInMinutes = value.toMinutes().clampToInt()
        }
    private var _workDurationInMinutes by intPref(Pomodoro.DEFAULT_WORK_DURATION_MINUTES, R.string.pref_work_duration)

    override var shortBreakDuration: Duration
        get() = Duration.ofMinutes(_shortBreakDurationInMinutes.toLong())
        set(value) {
            _workDurationInMinutes = value.toMinutes().clampToInt()
        }
    private var _shortBreakDurationInMinutes by intPref(Pomodoro.DEFAULT_SHORT_BREAK_DURATION, R.string.pref_short_break_duration)

    override var longBreakDuration: Duration
        get() = Duration.ofMinutes(_longBreakDurationInMinutes.toLong())
        set(value) {
            _longBreakDurationInMinutes = value.toMinutes().clampToInt()
        }
    private var _longBreakDurationInMinutes by intPref(Pomodoro.DEFAULT_LONG_BREAK_DURATION, R.string.pref_long_break_duration)

    override var longBreaksAreEnabled by booleanPref(true, R.string.pref_long_breaks_are_enabled)

    override var numberOfSessionsBetweenLongBreaks by intPref(Pomodoro.DEFAULT_LONG_BREAK_FREQUENCY, R.string.pref_long_break_frequency)

    // OnSharedPreferenceChangeListeners are stored in a WeakHashMap, so we need to store
    // an explicit reference to the listener to prevent it from being garbage-collected.
    // This object literal can't be replaced with a lambda because SAM conversion will
    // create a local object wrapping the lambda function, so there will be no long-living
    // reference to the listener and it would be garbage-collected.
    @Suppress("ObjectLiteralToLambda")
    private val prefsListener = object : SharedPreferences.OnSharedPreferenceChangeListener {

        private val pomodoroSettingKeys = setOf(
                context.getString(R.string.pref_work_duration),
                context.getString(R.string.pref_short_break_duration),
                context.getString(R.string.pref_long_break_duration),
                context.getString(R.string.pref_long_break_frequency),
                context.getString(R.string.pref_long_breaks_are_enabled)
        )

        override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
            Timber.d("onPreferenceChange() key=$key")
            if (key in pomodoroSettingKeys) {
                notifyAboutPomodoroSettingChange()
            }
        }
    }

    private var observers = mutableListOf<Observer>()

    override fun addObserver(observer: Observer) {
        if (observers.isEmpty()) {
            preferences.registerOnSharedPreferenceChangeListener(prefsListener)
        }
        observers = observers.toMutableList().apply {
            add(observer)
        }
    }

    override fun removeObserver(observer: Observer) {
        observers = observers.toMutableList().apply {
            remove(observer)
        }
        if (observers.isEmpty()) {
            preferences.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }

    private fun notifyAboutPomodoroSettingChange() {
        observers.forEach { it.onSettingsChange() }
    }

    private fun Long.clampToInt(): Int {
        return when {
            this > Int.MAX_VALUE -> Int.MAX_VALUE
            this < Int.MIN_VALUE -> Int.MIN_VALUE
            else -> toInt()
        }
    }
}