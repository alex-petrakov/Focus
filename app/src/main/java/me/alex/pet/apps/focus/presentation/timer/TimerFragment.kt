package me.alex.pet.apps.focus.presentation.timer

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.text.Annotation
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.text.getSpans
import androidx.core.text.set
import androidx.core.text.toSpanned
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialFadeThrough
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.extensions.excludeTargets
import me.alex.pet.apps.focus.common.extensions.getColorCompat
import me.alex.pet.apps.focus.common.extensions.observe
import me.alex.pet.apps.focus.common.extensions.requireAppContext
import me.alex.pet.apps.focus.databinding.FragmentTimerBinding
import me.alex.pet.apps.focus.presentation.HostActivity
import me.alex.pet.apps.focus.presentation.notificationservice.NotificationService
import org.koin.androidx.viewmodel.ext.android.viewModel

private typealias Icon = ViewState.ToggleButton.Icon

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null

    private val binding get() = _binding!!

    private val model by viewModel<TimerModel>()

    private var lastState: ViewState? = null

    private val iconTransitionDrawables: Map<Icon, Map<Icon, Int>> = mapOf(
            Icon.START to mapOf(
                    Icon.PAUSE to R.drawable.ic_start_to_pause
            ),
            Icon.PAUSE to mapOf(
                    Icon.START to R.drawable.ic_pause_to_start,
                    Icon.SWITCH_SESSION to R.drawable.ic_pause_to_hourglass
            ),
            Icon.SWITCH_SESSION to mapOf(
                    Icon.START to R.drawable.ic_hourglass_to_start
            )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpTransitions()
    }

    private fun setUpTransitions() {
        val mainContentTransition = MaterialFadeThrough().excludeTargets(
                R.id.toolbar,
                R.id.completed_sessions_tv,
                R.id.session_type_iv
        )
        val toolbarTransition = MaterialFade().apply {
            secondaryAnimatorProvider = null
            excludeTarget(R.id.root, true)
            excludeTarget(R.id.toolbar, false)
        }
        exitTransition = TransitionSet().addTransition(mainContentTransition)
                .addTransition(toolbarTransition)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            toolbar.inflateMenu(R.menu.menu_timer)
            workIntroTv.text = requireContext().getStyledSpannable(R.string.app_ready_to_start_a_work_session)
            breakIntroTv.text = requireContext().getStyledSpannable(R.string.app_ready_to_take_a_break)
        }
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
            blink(state.timer.isBlinking)
        }

        workIntroTv.isVisible = state.workIntro.isVisible

        breakIntroTv.isVisible = state.breakIntro.isVisible

        completedSessionsTv.apply {
            text = state.sessionCount.text
            isVisible = state.sessionCount.isVisible
        }

        sessionTypeIv.apply {
            setImageDrawable(requireContext().getDrawable(state.sessionIndicator.iconRes))
            isVisible = state.sessionIndicator.isVisible
        }

        toggleFab.apply {
            setImageResource(state.toggleButton.action.icon.iconRes)
            contentDescription = getString(state.toggleButton.action.textRes)
        }

        resetBtn.apply {
            isVisible = state.resetButton.isVisible
        }
    }

    private fun renderWithAnimations(state: ViewState) = binding.apply {
        val smallViewsFade = MaterialFade().setDuration(200)
                .excludeTargets(timerTv, workIntroTv, breakIntroTv, toggleFab, toolbar)
        val primaryViewsFadeThrough = MaterialFadeThrough().setDuration(400)
                .excludeTargets(resetBtn, toggleFab, toolbar, completedSessionsTv, sessionTypeIv)
        TransitionSet().setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(smallViewsFade)
                .addTransition(primaryViewsFadeThrough)
                .let { TransitionManager.beginDelayedTransition(root, it) }

        timerTv.apply {
            isVisible = state.timer.isVisible
            text = state.timer.text
            blink(state.timer.isBlinking)
        }

        workIntroTv.isVisible = state.workIntro.isVisible

        breakIntroTv.isVisible = state.breakIntro.isVisible

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

        diff(lastState, state, ViewState::toggleButton) { toggle ->
            val oldIcon = lastState?.toggleButton?.action?.icon
            val newIcon = toggle.action.icon
            val animatedDrawableRes = getAnimatedDrawableRes(oldIcon, newIcon)
            if (animatedDrawableRes != null) {
                toggleFab.setImageResource(animatedDrawableRes)
                (toggleFab.drawable as Animatable).start()
            } else {
                toggleFab.setImageResource(newIcon.iconRes)
            }

            toggleFab.contentDescription = getString(toggle.action.textRes)
        }

        diff(lastState, state, ViewState::resetButton) { resetButton ->
            resetBtn.isVisible = resetButton.isVisible
        }
    }

    @DrawableRes
    private fun getAnimatedDrawableRes(oldIcon: Icon?, newIcon: Icon): Int? {
        return iconTransitionDrawables[oldIcon]?.get(newIcon)
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

private fun Context.getStyledSpannable(@StringRes id: Int): Spannable {
    val originalText = this.getText(id).toSpanned()
    val styledSpannable = SpannableString(originalText)
    originalText.getSpans<Annotation>().forEach { annotation ->
        if (annotation.key == TextEmphasis.KEY) {
            // TODO: Extract the color from the app theme
            val color = getColorCompat(TextEmphasis.from(annotation.value).colorResId)
            val selectionColor = ColorUtils.setAlphaComponent(color, 24 * 255 / 100)
            val start = originalText.getSpanStart(annotation)
            val end = originalText.getSpanEnd(annotation)
            styledSpannable[start, end] = ForegroundColorSpan(color)
            styledSpannable[start, end] = BackgroundColorSpan(selectionColor)
        }
    }
    return styledSpannable
}

private enum class TextEmphasis(val key: String, @ColorRes val colorResId: Int) {
    FOCUS("focus", R.color.colorFocus),
    REST("rest", R.color.colorRest);

    companion object {
        const val KEY = "emphasis"

        fun from(value: String): TextEmphasis {
            return values().find { it.key == value }
                    ?: throw IllegalArgumentException("Unknown emphasis style: $value")
        }
    }
}

private fun View.blink(blink: Boolean) {
    when {
        blink -> this.startBlinking()
        else -> this.stopBlinking()
    }
}

private fun View.startBlinking() {
    val animator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.2f).apply {
        repeatMode = ObjectAnimator.REVERSE
        repeatCount = ObjectAnimator.INFINITE
        duration = 1000L
        interpolator = LinearInterpolator()
        setAutoCancel(true)
        start()
    }
    this.tag = animator
}

private fun View.stopBlinking() {
    val viewTag = this.tag
    if (viewTag is ObjectAnimator) {
        viewTag.cancel()
        this.apply {
            alpha = 1f
            tag = null
        }
    }
}