package me.alex.pet.apps.focus.presentation.timer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import me.alex.pet.apps.focus.common.extensions.observe
import me.alex.pet.apps.focus.common.extensions.requireAppContext
import me.alex.pet.apps.focus.databinding.FragmentTimerBinding
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
            timerLayout.apply {
                toggleFab.setOnClickListener { model.onToggleTimer() }
                resetBtn.setOnClickListener { model.onReset() }
            }
            workIntroLayout.apply {
                startWorkSessionBtn.setOnClickListener { model.onSwitchToNextSession() }
                resetBtn.setOnClickListener { model.onReset() }
            }
            breakIntroLayout.apply {
                startWorkSessionBtn.setOnClickListener { model.onSwitchToNextSession() }
                resetBtn.setOnClickListener { model.onReset() }
            }
        }
    }

    private fun renderState(state: ViewState) {
        binding.apply {
            timerLayout.apply {
                timerTv.text = state.timer.text

                completedSessionsTv.text = state.sessionCount.text
                completedSessionsTv.isVisible = state.sessionCount.isVisible

                toggleFab.setImageDrawable(state.toggle.icon)
                toggleFab.contentDescription = state.toggle.text

                resetBtn.isVisible = state.resetBtnIsVisible

                root.isVisible = state.visiblePanel == ViewState.Panel.TIMER
            }
            workIntroLayout.root.isVisible = state.visiblePanel == ViewState.Panel.WORK_INTRO
            breakIntroLayout.root.isVisible = state.visiblePanel == ViewState.Panel.BREAK_INTRO
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