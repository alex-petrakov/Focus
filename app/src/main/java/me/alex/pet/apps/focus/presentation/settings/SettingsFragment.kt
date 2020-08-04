package me.alex.pet.apps.focus.presentation.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import me.alex.pet.apps.focus.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.prefs_app)
        setPreferencesFromResource(R.xml.prefs_app, rootKey)
    }
}