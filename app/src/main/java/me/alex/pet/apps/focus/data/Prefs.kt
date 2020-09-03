package me.alex.pet.apps.focus.data

import android.content.Context
import android.content.SharedPreferences
import com.chibatching.kotpref.KotprefModel
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.PomodoroSettingsRepository
import me.alex.pet.apps.focus.domain.PomodoroSettingsRepository.Observer
import timber.log.Timber
import java.time.Duration

class Prefs(context: Context) : KotprefModel(context), PomodoroSettingsRepository {

    override val kotprefName = context.getString(R.string.prefs_app)

    private val workDurationKey = context.getString(R.string.pref_work_duration)
    private val shortBreakDurationKey = context.getString(R.string.pref_short_break_duration)
    private val longBreakDurationKey = context.getString(R.string.pref_long_break_duration)
    private val longBreaksAreEnabledKey = context.getString(R.string.pref_long_breaks_are_enabled)
    private val longBreakFrequencyKey = context.getString(R.string.pref_long_break_frequency)

    private val pomodoroSettingKeys = setOf(
            workDurationKey,
            shortBreakDurationKey,
            longBreakDurationKey,
            longBreaksAreEnabledKey,
            longBreakFrequencyKey
    )

    override val settings: Pomodoro.Settings
        get() = Pomodoro.Settings(
                workDuration,
                shortBreakDuration,
                longBreakDuration,
                longBreaksAreEnabled,
                numberOfSessionsBetweenLongBreaks
        )

    override var workDuration: Duration
        get() = Duration.ofMinutes(_workDurationInMinutes.toLong())
        set(value) {
            _workDurationInMinutes = value.toMinutes().clampToInt()
        }
    private var _workDurationInMinutes by intPref(default = Pomodoro.DEFAULT_WORK_DURATION_MINUTES, key = workDurationKey)

    override var shortBreakDuration: Duration
        get() = Duration.ofMinutes(_shortBreakDurationInMinutes.toLong())
        set(value) {
            _workDurationInMinutes = value.toMinutes().clampToInt()
        }
    private var _shortBreakDurationInMinutes by intPref(default = Pomodoro.DEFAULT_SHORT_BREAK_DURATION, key = shortBreakDurationKey)

    override var longBreakDuration: Duration
        get() = Duration.ofMinutes(_longBreakDurationInMinutes.toLong())
        set(value) {
            _longBreakDurationInMinutes = value.toMinutes().clampToInt()
        }
    private var _longBreakDurationInMinutes by intPref(default = Pomodoro.DEFAULT_LONG_BREAK_DURATION, key = longBreakDurationKey)

    override var longBreaksAreEnabled by booleanPref(default = true, key = longBreaksAreEnabledKey)

    override var numberOfSessionsBetweenLongBreaks by intPref(default = Pomodoro.DEFAULT_LONG_BREAK_FREQUENCY, key = longBreakFrequencyKey)

    // OnSharedPreferenceChangeListeners are stored in a WeakHashMap, so we need to store
    // an explicit reference to the listener to prevent it from being garbage-collected.
    // This object literal can't be replaced with a lambda because SAM conversion will
    // create a local object wrapping the lambda function, so there will be no long-living
    // reference to the listener and it would be garbage-collected.
    @Suppress("ObjectLiteralToLambda")
    private val prefsListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
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
        val settings = settings
        observers.forEach { it.onSettingsChange(settings) }
    }

    private fun Long.clampToInt(): Int {
        return when {
            this > Int.MAX_VALUE -> Int.MAX_VALUE
            this < Int.MIN_VALUE -> Int.MIN_VALUE
            else -> toInt()
        }
    }
}