package me.alex.pet.apps.focus.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.databinding.ActivityHostBinding
import me.alex.pet.apps.focus.presentation.settings.SettingsFragment
import me.alex.pet.apps.focus.presentation.timer.TimerFragment

class HostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, TimerFragment())
                    .commit()
        }
    }

    fun navigateToSettings() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
    }
}