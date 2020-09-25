package me.alex.pet.apps.focus.presentation.timer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFade
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

    private var lastState: ViewState? = null

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
        when (lastState) {
            null -> renderWithoutAnimations(state)
            else -> renderWithAnimations(state)
        }

        lastState = state
    }

    private fun renderWithoutAnimations(state: ViewState) = binding.apply {
        timerTv.apply {
            isVisible = state.timer.isVisible
            text = state.timer.text
        }

        sessionSwitchPromptTv.apply {
            isVisible = state.transitionPrompt.isVisible
            text = state.transitionPrompt.text
        }

        completedSessionsTv.apply {
            text = state.sessionCount.text
            isVisible = state.sessionCount.isVisible
        }

        sessionTypeIv.apply {
            setImageDrawable(requireContext().getDrawable(state.sessionIndicator.iconRes))
            isVisible = state.sessionIndicator.isVisible
        }

        toggleFab.apply {
            setImageResource(state.toggleButton.action.iconRes)
            contentDescription = getString(state.toggleButton.action.textRes)
        }

        resetBtn.apply {
            isVisible = state.resetButton.isVisible
        }
    }

    private fun renderWithAnimations(state: ViewState) = binding.apply {
        MaterialFade().setDuration(200)
                .excludeTargets(timerTv, sessionSwitchPromptTv, toggleFab, toolbar)
                .let { fade -> TransitionManager.beginDelayedTransition(root, fade) }

        timerTv.apply {
            isVisible = state.timer.isVisible
            text = state.timer.text
        }

        sessionSwitchPromptTv.apply {
            isVisible = state.transitionPrompt.isVisible
            text = state.transitionPrompt.text
        }

        diff(lastState, state, ViewState::sessionCount) { count ->
            completedSessionsTv.apply {
                text = count.text
                isVisible = count.isVisible
            }
        }

        diff(lastState, state, ViewState::sessionIndicator) { indicator ->
            sessionTypeIv.apply {
                setImageDrawable(requireContext().getDrawable(indicator.iconRes))
                isVisible = indicator.isVisible
            }
        }

        toggleFab.apply {
            setImageResource(state.toggleButton.action.iconRes)
            contentDescription = getString(state.toggleButton.action.textRes)
        }

        diff(lastState, state, ViewState::resetButton) { resetButton ->
            resetBtn.isVisible = resetButton.isVisible
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
        lastState = null
    }
}


private inline fun <S, P> diff(oldState: S?, newState: S, getPropertyFrom: (S) -> P, onDiff: (P) -> Unit) {
    val newValue = getPropertyFrom(newState)
    when (oldState) {
        null -> onDiff(newValue)
        else -> {
            val oldValue = getPropertyFrom(oldState)
            if (oldValue != newValue) {
                onDiff(newValue)
            }
        }
    }
}

private fun Transition.excludeTargets(vararg targets: View, exclude: Boolean = true): Transition {
    targets.forEach { view ->
        excludeTarget(view, exclude)
    }
    return this
}