package me.alex.pet.apps.focus.presentation.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.*
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.extensions.toIntMinutes
import me.alex.pet.apps.focus.domain.Pomodoro

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.prefs_app)
        val context = preferenceManager.context

        val pomodoroOptionsCategory = PreferenceCategory(context).apply {
            title = getString(R.string.settings_category_pomodoro_options)
            key = getString(R.string.pref_category_pomodoro_options)
        }

        preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
            addPreference(pomodoroOptionsCategory)
        }
        pomodoroOptionsCategory.addPomodoroOptionPreferences()
    }
}


private fun PreferenceCategory.addPomodoroOptionPreferences() {
    addPreference(createWorkDurationPreference(context))
    addPreference(createShortBreakDurationPreference(context))

    val longBreakOnOffPreference = createLongBreakOnOffPreference(context).also {
        addPreference(it)
    }

    createLongBreakDurationPreference(context).let { pref ->
        addPreference(pref)
        pref.dependency = longBreakOnOffPreference.key
    }

    createLongBreakFrequencyPreference(context).let { pref ->
        addPreference(pref)
        pref.dependency = longBreakOnOffPreference.key
    }
}

private fun createWorkDurationPreference(context: Context): Preference {
    return SeekBarPreference(context).apply {
        key = context.getString(R.string.pref_work_duration)
        title = context.getString(R.string.settings_focus_session_duration)
        min = 15
        max = 90
        setDefaultValue(Pomodoro.defaultWorkDuration.toIntMinutes())
        showSeekBarValue = true
    }
}

private fun createShortBreakDurationPreference(context: Context): Preference {
    return SeekBarPreference(context).apply {
        key = context.getString(R.string.pref_short_break_duration)
        title = context.getString(R.string.settings_short_break_duration)
        min = 1
        max = 30
        setDefaultValue(Pomodoro.defaultShortBreakDuration.toIntMinutes())
        showSeekBarValue = true
    }
}

private fun createLongBreakOnOffPreference(context: Context): Preference {
    return SwitchPreference(context).apply {
        key = context.getString(R.string.pref_long_breaks_are_enabled)
        title = context.getString(R.string.settings_enable_long_breaks)
        setDefaultValue(Pomodoro.defaultLongBreaksAreEnabled)
    }
}

private fun createLongBreakDurationPreference(context: Context): Preference {
    return SeekBarPreference(context).apply {
        key = context.getString(R.string.pref_long_break_duration)
        title = context.getString(R.string.settings_long_break_duration)
        min = 1
        max = 30
        setDefaultValue(Pomodoro.defaultLongBreakDuration.toIntMinutes())
        showSeekBarValue = true
    }
}

private fun createLongBreakFrequencyPreference(context: Context): Preference {
    return SeekBarPreference(context).apply {
        key = context.getString(R.string.pref_long_break_frequency)
        title = context.getString(R.string.settings_session_count_between_long_breaks)
        min = 1
        max = 10
        setDefaultValue(Pomodoro.defaultLongBreakFrequency)
        showSeekBarValue = true
    }
}