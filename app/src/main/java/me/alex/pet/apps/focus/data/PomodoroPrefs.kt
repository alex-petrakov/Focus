package me.alex.pet.apps.focus.data

import android.content.Context
import android.content.SharedPreferences
import com.chibatching.kotpref.KotprefModel
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.extensions.toInt
import me.alex.pet.apps.focus.common.extensions.toIntMinutes
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.PomodoroSettings
import me.alex.pet.apps.focus.domain.PomodoroSettings.Observer
import timber.log.Timber
import java.time.Duration
import java.time.temporal.ChronoUnit

class PomodoroPrefs(context: Context) : KotprefModel(context), PomodoroSettings {

    override val kotprefName = context.getString(R.string.prefs_app)

    override var workDuration: Duration
        get() = Duration.of(workDurationValue.toLong(), timeUnit)
        set(value) {
            workDurationValue = value.toInt(timeUnit)
        }
    private var workDurationValue by intPref(Pomodoro.DEFAULT_WORK_DURATION.toIntMinutes(), R.string.pref_work_duration)

    override var shortBreakDuration: Duration
        get() = Duration.of(shortBreakDurationValue.toLong(), timeUnit)
        set(value) {
            shortBreakDurationValue = value.toInt(timeUnit)
        }
    private var shortBreakDurationValue by intPref(Pomodoro.DEFAULT_SHORT_BREAK_DURATION.toIntMinutes(), R.string.pref_short_break_duration)

    override var longBreakDuration: Duration
        get() = Duration.of(longBreakDurationValue.toLong(), timeUnit)
        set(value) {
            longBreakDurationValue = value.toInt(timeUnit)
        }
    private var longBreakDurationValue by intPref(Pomodoro.DEFAULT_LONG_BREAK_DURATION.toIntMinutes(), R.string.pref_long_break_duration)

    override var longBreaksAreEnabled by booleanPref(Pomodoro.DEFAULT_LONG_BREAKS_ARE_ENABLED, R.string.pref_long_breaks_are_enabled)

    override var numberOfSessionsBetweenLongBreaks by intPref(Pomodoro.DEFAULT_LONG_BREAK_FREQUENCY, R.string.pref_long_break_frequency)

    private val sessionShorteningIsEnabled by booleanPref(DevOptions.DEFAULT_SESSION_SHORTENING_ON_OFF, R.string.pref_session_shortening_on_off)

    private val timeUnit
        get() = when (sessionShorteningIsEnabled) {
            true -> ChronoUnit.SECONDS
            else -> ChronoUnit.MINUTES
        }

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
                context.getString(R.string.pref_long_breaks_are_enabled),
                context.getString(R.string.pref_session_shortening_on_off)
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

    object DevOptions {
        const val DEFAULT_SESSION_SHORTENING_ON_OFF = false
    }
}