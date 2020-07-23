package me.alex.pet.apps.focus.presentation.timer

import android.graphics.drawable.Drawable

data class ViewState(
        val timer: Timer,
        val progress: Progress,
        val sessionCount: SessionCount,
        val toggle: Toggle,
        val resetBtnIsVisible: Boolean,
        val visiblePanel: Panel
) {

    enum class Panel {
        TIMER,
        WORK_INTRO,
        BREAK_INTRO
    }

    data class Timer(
            val text: String,
            val isBlinking: Boolean
    )

    data class Progress(
            val isVisible: Boolean,
            val value: Int,
            val color: Int
    )

    data class SessionCount(
            val isVisible: Boolean,
            val text: String
    )

    data class Toggle(
            val isVisible: Boolean,
            val text: String,
            val icon: Drawable
    )
}