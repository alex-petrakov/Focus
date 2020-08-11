package me.alex.pet.apps.focus.data

import android.content.Context
import android.content.SharedPreferences
import com.chibatching.kotpref.KotprefModel
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.PomodoroConfigurationRepository
import me.alex.pet.apps.focus.domain.PomodoroConfigurationRepository.PomodoroConfigurationObserver
import timber.log.Timber

class Prefs(context: Context) : KotprefModel(context), PomodoroConfigurationRepository {

    override val kotprefName = context.getString(R.string.prefs_app)

    private val workDurationKey = context.getString(R.string.pref_work_duration)
    private val shortBreakDurationKey = context.getString(R.string.pref_short_break_duration)
    private val longBreakDurationKey = context.getString(R.string.pref_long_break_duration)
    private val longBreakAreEnabledKey = context.getString(R.string.pref_long_breaks_are_enabled)
    private val longBreakFrequencyKey = context.getString(R.string.pref_long_break_frequency)
    private val sessionAutoSwitchKey = context.getString(R.string.pref_auto_session_switch_is_on)

    override var workDurationMins by intPref(default = Pomodoro.DEFAULT_WORK_DURATION_MINUTES, key = workDurationKey)

    override var shortBreakDurationMins by intPref(default = Pomodoro.DEFAULT_SHORT_BREAK_DURATION, key = shortBreakDurationKey)

    override var longBreakDurationMins by intPref(default = Pomodoro.DEFAULT_LONG_BREAK_DURATION, key = longBreakDurationKey)

    override var longBreaksAreEnabled by booleanPref(default = true, key = longBreakAreEnabledKey)

    override var numberOfSessionsBetweenLongBreaks by intPref(default = Pomodoro.DEFAULT_LONG_BREAK_FREQUENCY, key = longBreakFrequencyKey)

    override var autoSessionSwitchIsEnabled by booleanPref(default = false, key = sessionAutoSwitchKey)

    // OnSharedPreferenceChangeListeners are stored in a WeakHashMap, so we need to store
    // an explicit reference to the listener to prevent it from being garbage-collected.
    // This object literal can't be replaced with a lambda because SAM conversion will
    // create a local object wrapping the lambda function, so there will be no long-living
    // reference to the listener and it would be garbage-collected.
    @Suppress("ObjectLiteralToLambda")
    private val prefsListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
            Timber.d("onPreferenceChange() key=$key")
            when (key) {
                workDurationKey -> notifyAboutWorkDurationChange()
                shortBreakDurationKey -> notifyAboutShortBreakDurationChange()
                longBreakDurationKey -> notifyAboutLongBreakDurationChange()
                longBreakAreEnabledKey -> notifyAboutLongBreakOnOff()
                longBreakFrequencyKey -> notifyAboutLongBreaksFrequencyChange()
                sessionAutoSwitchKey -> notifyAboutAutoSessionSwitchOnOff()
            }
        }
    }

    private val observers = mutableListOf<PomodoroConfigurationObserver>()

    override fun addPomodoroConfigurationObserver(observer: PomodoroConfigurationObserver) {
        if (observers.isEmpty()) {
            preferences.registerOnSharedPreferenceChangeListener(prefsListener)
        }
        observers.add(observer)
    }

    override fun removePomodoroConfigurationObserver(observer: PomodoroConfigurationObserver) {
        observers.remove(observer)
        if (observers.isEmpty()) {
            preferences.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }

    private fun notifyAboutWorkDurationChange() {
        val duration = workDurationMins
        observers.forEach { it.onWorkDurationChange(duration) }
    }

    private fun notifyAboutShortBreakDurationChange() {
        val duration = shortBreakDurationMins
        observers.forEach { it.onShortBreakDurationChange(duration) }
    }

    private fun notifyAboutLongBreakDurationChange() {
        val duration = longBreakDurationMins
        observers.forEach { it.onLongBreakDurationChange(duration) }
    }

    private fun notifyAboutLongBreakOnOff() {
        val longBreaksAreEnabled = longBreaksAreEnabled
        observers.forEach { it.onLongBreaksEnabled(longBreaksAreEnabled) }
    }

    private fun notifyAboutLongBreaksFrequencyChange() {
        val frequency = numberOfSessionsBetweenLongBreaks
        observers.forEach { it.onLongBreakFrequencyChange(frequency) }
    }

    private fun notifyAboutAutoSessionSwitchOnOff() {
        val autoSessionSwitchIsEnabled = autoSessionSwitchIsEnabled
        observers.forEach { it.onAutoSessionSwitchEnabled(autoSessionSwitchIsEnabled) }
    }
}