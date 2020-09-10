package me.alex.pet.apps.focus.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.edit
import androidx.preference.*
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.extensions.toIntMinutes
import me.alex.pet.apps.focus.data.NotificationPrefs
import me.alex.pet.apps.focus.domain.Pomodoro

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var soundPreference: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.prefs_app)

        preferenceScreen = preferenceManager.createPreferenceScreen(context)

        val context = preferenceManager.context
        createPomodoroPreferenceCategory(context)
        createNotificationPreferenceCategory(context)
    }

    private fun createPomodoroPreferenceCategory(context: Context) {
        PreferenceCategory(context).let { category ->
            category.title = getString(R.string.settings_category_pomodoro_options)
            category.key = getString(R.string.pref_category_pomodoro_options)
            preferenceScreen.addPreference(category)

            category.addPreference(createWorkDurationPreference(context))
            category.addPreference(createShortBreakDurationPreference(context))

            val longBreakOnOffPreference = createLongBreakOnOffPreference(context).also {
                category.addPreference(it)
            }

            createLongBreakDurationPreference(context).let { pref ->
                category.addPreference(pref)
                pref.dependency = longBreakOnOffPreference.key
            }

            createLongBreakFrequencyPreference(context).let { pref ->
                category.addPreference(pref)
                pref.dependency = longBreakOnOffPreference.key
            }
        }
    }

    private fun createNotificationPreferenceCategory(context: Context) {
        PreferenceCategory(context).let { category ->
            category.title = getString(R.string.settings_category_notifications)
            category.key = getString(R.string.pref_category_notification)
            preferenceScreen.addPreference(category)

            val soundOnOffPreference = createSoundOnOffPreference(context).also { pref ->
                category.addPreference(pref)
            }

            soundPreference = createSoundPreference(context).also { pref ->
                category.addPreference(pref)
                pref.dependency = soundOnOffPreference.key

                val storedUriString = pref.sharedPreferences.getString(pref.key, NotificationPrefs.defaultNotificationSoundUri.toString())
                val ringtoneUri = Uri.parse(storedUriString)
                val ringtoneTitle = RingtoneManager.getRingtone(context, ringtoneUri).getTitle(context)
                pref.summary = ringtoneTitle
            }

            createVibrationOnOffPreference(context).also { pref ->
                category.addPreference(pref)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        soundPreference.setOnPreferenceClickListener { preference ->
            val pickerTitle = preference.context.getString(R.string.settings_notification_sound)

            val storedUriString = preference.sharedPreferences.getString(preference.key, NotificationPrefs.defaultNotificationSoundUri.toString())
            val ringtoneUri = Uri.parse(storedUriString)

            val intent = createRingtonePickerIntent(pickerTitle, ringtoneUri)
            startActivityForResult(intent, RequestCode.ringtone)
            true
        }
    }

    override fun onStop() {
        soundPreference.onPreferenceClickListener = null
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ringtone -> handleRingtonePickerResult(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleRingtonePickerResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                val ringtoneUri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                        ?: return
                val ringtoneTitle = RingtoneManager.getRingtone(requireContext(), ringtoneUri).getTitle(requireContext())

                preferenceManager.sharedPreferences.edit {
                    putString(getString(R.string.pref_sound), ringtoneUri.toString())
                }

                val soundPreference = preferenceManager.findPreference<Preference>(getString(R.string.pref_sound))!!
                soundPreference.summary = ringtoneTitle
            }
            else -> return
        }
    }


    private object RequestCode {
        const val ringtone: Int = 100
    }
}


private fun createRingtonePickerIntent(pickerTitle: String, pickedRingtoneUri: Uri): Intent {
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, pickerTitle)
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, pickedRingtoneUri)
        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
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

private fun createSoundOnOffPreference(context: Context): Preference {
    return SwitchPreference(context).apply {
        key = context.getString(R.string.pref_sound_on_off)
        title = context.getString(R.string.settings_enable_notification_sound)
        setDefaultValue(NotificationPrefs.defaultSoundIsEnabled)
    }
}

private fun createSoundPreference(context: Context): Preference {
    return Preference(context).apply {
        key = context.getString(R.string.pref_sound)
        title = context.getString(R.string.settings_notification_sound)
        setDefaultValue(NotificationPrefs.defaultNotificationSoundUri)
    }
}

private fun createVibrationOnOffPreference(context: Context): Preference {
    return SwitchPreference(context).apply {
        key = context.getString(R.string.pref_vibration_on_off)
        title = context.getString(R.string.settings_enable_vibration)
        setDefaultValue(NotificationPrefs.defaultVibrationIsEnabled)
    }
}