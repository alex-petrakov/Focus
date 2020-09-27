package me.alex.pet.apps.focus.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionSet
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialFadeThrough
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.extensions.excludeTargets
import me.alex.pet.apps.focus.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpTransitions()
    }

    private fun setUpTransitions() {
        val mainContentTransition = MaterialFadeThrough().excludeTargets(R.id.toolbar)
        val toolbarTransition = MaterialFade().apply {
            secondaryAnimatorProvider = null
            excludeTarget(R.id.root, true)
            excludeTarget(R.id.toolbar, false)
        }
        enterTransition = TransitionSet().addTransition(mainContentTransition)
                .addTransition(toolbarTransition)
    }

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