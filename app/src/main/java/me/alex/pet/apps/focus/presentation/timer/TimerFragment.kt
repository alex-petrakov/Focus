package me.alex.pet.apps.focus.presentation.timer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.extensions.observe
import me.alex.pet.apps.focus.common.extensions.requireAppContext
import me.alex.pet.apps.focus.databinding.FragmentTimerBinding
import me.alex.pet.apps.focus.presentation.HostActivity
import me.alex.pet.apps.focus.presentation.notificationservice.NotificationService
import org.koin.androidx.viewmodel.ext.android.viewModel

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null

    private val binding get() = _binding!!

    private val model by viewModel<TimerModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.inflateMenu(R.menu.menu_timer)
        subscribeToModel()
    }

    private fun subscribeToModel() {
        model.apply {
            viewState.observe(viewLifecycleOwner) { newState -> renderState(newState) }
            viewEffect.observe(viewLifecycleOwner) { effect -> handleEffect(effect) }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            toolbar.setOnMenuItemClickListener(::handleMenuItemClick)
            toggleFab.setOnClickListener { model.onToggleTimer() }
            resetBtn.setOnClickListener { model.onReset() }
        }
    }

    private fun handleMenuItemClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_open_settings -> {
                (requireActivity() as HostActivity).navigateToSettings()
                true
            }
            else -> false
        }
    }

    private fun renderState(state: ViewState) {
        binding.apply {
            timerTv.isVisible = state.timer.isVisible
            timerTv.text = state.timer.text

            sessionSwitchPromptTv.isVisible = state.transitionPrompt.isVisible
            sessionSwitchPromptTv.text = state.transitionPrompt.text

            completedSessionsTv.text = state.sessionCount.text
            completedSessionsTv.isVisible = state.sessionCount.isVisible

            sessionTypeIv.setImageDrawable(requireContext().getDrawable(state.sessionIndicator.iconRes))
            sessionTypeIv.isVisible = state.sessionIndicator.isVisible

            toggleFab.setImageResource(state.toggleButton.action.iconRes)
            toggleFab.contentDescription = getString(state.toggleButton.action.textRes)

            resetBtn.isVisible = state.resetButton.isVisible
        }
    }

    private fun handleEffect(effect: ViewEffect) {
        when (effect) {
            ViewEffect.START_NOTIFICATIONS -> startNotificationService()
        }
    }

    private fun startNotificationService() {
        requireAppContext().also { context ->
            val intent = Intent(context, NotificationService::class.java)
            context.startService(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}