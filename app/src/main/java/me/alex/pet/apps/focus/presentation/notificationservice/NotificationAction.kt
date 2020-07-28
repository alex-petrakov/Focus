package me.alex.pet.apps.focus.presentation.notificationservice

import me.alex.pet.apps.focus.BuildConfig

enum class NotificationAction(val value: String) {
    PAUSE("${BuildConfig.APPLICATION_ID}.PAUSE"),
    RESUME("${BuildConfig.APPLICATION_ID}.RESUME"),
    SWITCH_TO_NEXT_SESSION("${BuildConfig.APPLICATION_ID}.SWITCH_SESSION"),
    RESET("${BuildConfig.APPLICATION_ID}.RESET");

    companion object {
        fun from(value: String?): NotificationAction {
            return values().find { it.value == value }
                    ?: throw IllegalArgumentException("Unknown id $value")
        }
    }
}