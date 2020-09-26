package me.alex.pet.apps.focus.presentation.timer

import androidx.annotation.DrawableRes
import me.alex.pet.apps.focus.R

data class ViewState(
        val timer: Timer,
        val workIntro: WorkIntro,
        val breakIntro: BreakIntro,
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

    data class WorkIntro(
            val isVisible: Boolean
    )

    data class BreakIntro(
            val isVisible: Boolean
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
        enum class Action(val textRes: Int, val icon: Icon) {
            START(R.string.app_action_start, Icon.START),
            PAUSE(R.string.app_action_pause, Icon.PAUSE),
            RESUME(R.string.app_action_resume, Icon.START),
            START_WORK(R.string.app_start_work_session, Icon.SWITCH_SESSION),
            START_BREAK(R.string.app_start_break, Icon.SWITCH_SESSION)
        }

        enum class Icon(@DrawableRes val iconRes: Int) {
            START(R.drawable.ic_action_start),
            PAUSE(R.drawable.ic_action_pause),
            SWITCH_SESSION(R.drawable.ic_action_switch_session)
        }
    }

    data class ResetButton(val isVisible: Boolean)
}