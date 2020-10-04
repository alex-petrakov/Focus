package me.alex.pet.apps.focus.presentation.notificationservice

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.alex.pet.apps.focus.BuildConfig
import me.alex.pet.apps.focus.R

enum class NotificationAction(val value: String, @DrawableRes val iconRes: Int, @StringRes val titleRes: Int) {
    PAUSE(
            "${BuildConfig.APPLICATION_ID}.PAUSE",
            R.drawable.ic_action_pause,
            R.string.app_action_pause
    ),
    RESUME(
            "${BuildConfig.APPLICATION_ID}.RESUME",
            R.drawable.ic_action_start,
            R.string.app_action_resume
    ),
    SWITCH_TO_WORK_SESSION(
            "${BuildConfig.APPLICATION_ID}.SWITCH_TO_WORK_SESSION",
            R.drawable.ic_action_switch_session,
            R.string.app_start_work_session
    ),
    SWITCH_TO_BREAK(
            "${BuildConfig.APPLICATION_ID}.SWITCH_TO_BREAK",
            R.drawable.ic_action_switch_session,
            R.string.app_start_break
    ),
    RESET(
            "${BuildConfig.APPLICATION_ID}.RESET",
            R.drawable.ic_action_reset,
            R.string.app_action_reset
    );

    companion object {
        fun from(value: String?): NotificationAction {
            return values().find { it.value == value }
                    ?: throw IllegalArgumentException("Unknown id $value")
        }
    }
}