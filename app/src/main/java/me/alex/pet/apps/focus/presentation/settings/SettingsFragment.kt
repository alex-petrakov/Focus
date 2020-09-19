package me.alex.pet.apps.focus.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.apply {
            setNavigationIcon(R.drawable.ic_action_up)
            setTitle(R.string.app_settings)
            setNavigationContentDescription(R.string.app_navigate_up)
        }
    }

    override fun onStart() {
        super.onStart()

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }
}