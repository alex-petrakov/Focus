package me.alex.pet.apps.focus.presentation.timer

import android.text.Spanned
import androidx.annotation.DrawableRes
import me.alex.pet.apps.focus.R

data class ViewState(
        val timer: Timer,
        val transitionPrompt: TransitionPrompt,
        val sessionCount: SessionCount,
        val sessionIndicator: SessionIndicator,
        val toggleButton: ToggleButton,
        val resetButton: ResetButton
) {

    data class Timer(
            val isVisible: Boolean,
            val text: String,
            val isBlinking: Boolean
    )

    data class TransitionPrompt(
            val isVisible: Boolean,
            val text: Spanned
    )

    data class SessionCount(
            val isVisible: Boolean,
            val text: String
    )

    data class SessionIndicator(
            val isVisible: Boolean,
            @DrawableRes val iconRes: Int
    )

    data class ToggleButton(
            val action: Action
    ) {
        enum class Action(val textRes: Int, val iconRes: Int) {
            START(R.string.app_action_start, R.drawable.ic_action_start),
            PAUSE(R.string.app_action_pause, R.drawable.ic_action_pause),
            RESUME(R.string.app_action_resume, R.drawable.ic_action_start),
            START_WORK(R.string.app_start_work_session, R.drawable.ic_action_switch_session),
            START_BREAK(R.string.app_start_break, R.drawable.ic_action_switch_session)
        }
    }

    data class ResetButton(val isVisible: Boolean)
}