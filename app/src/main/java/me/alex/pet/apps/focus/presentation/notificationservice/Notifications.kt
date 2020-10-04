package me.alex.pet.apps.focus.presentation.notificationservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.alex.pet.apps.focus.R
import me.alex.pet.apps.focus.common.extensions.getColorCompat
import me.alex.pet.apps.focus.domain.SessionType
import me.alex.pet.apps.focus.domain.Timer
import me.alex.pet.apps.focus.presentation.HostActivity


class Notifications(private val context: Context) {

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.app_timer_notification_channel_name)
            val descriptionText = context.getString(R.string.app_timer_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(TIMER_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setBypassDnd(true)
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }

    fun newWorkIntroNotification(): Notification {
        // TODO: consider reusing actions and corresponding intents
        val resetAction = newAction(NotificationAction.RESET, R.drawable.ic_action_reset, context.getString(R.string.app_action_reset))
        val sessionSwitchAction = newAction(NotificationAction.SWITCH_TO_NEXT_SESSION, R.drawable.ic_action_switch_session, context.getString(R.string.app_start_work_session))
        return newPomodoroNotificationBuilder()
                .setContentTitle(context.getString(R.string.notifications_break_is_over))
                .setContentText(context.getString(R.string.app_ready_to_start_a_work_session))
                .addAction(sessionSwitchAction)
                .addAction(resetAction)
                .build()
    }

    fun newBreakIntroNotification(): Notification {
        val resetAction = newAction(NotificationAction.RESET, R.drawable.ic_action_reset, context.getString(R.string.app_action_reset))
        val sessionSwitchAction = newAction(NotificationAction.SWITCH_TO_NEXT_SESSION, R.drawable.ic_action_switch_session, context.getString(R.string.app_start_break))
        return newPomodoroNotificationBuilder()
                .setContentTitle(context.getString(R.string.notifications_work_session_is_finished))
                .setContentText(context.getString(R.string.app_ready_to_take_a_break))
                .addAction(sessionSwitchAction)
                .addAction(resetAction)
                .build()
    }

    fun newTimerNotification(type: SessionType, timerState: Timer.State, remainingSeconds: Long): Notification {
        val pauseAction = newAction(NotificationAction.PAUSE, R.drawable.ic_action_pause, context.getString(R.string.app_action_pause))
        val resumeAction = newAction(NotificationAction.RESUME, R.drawable.ic_action_start, context.getString(R.string.app_action_resume))
        val resetAction = newAction(NotificationAction.RESET, R.drawable.ic_action_reset, context.getString(R.string.app_action_reset))
        val actions = when (timerState) {
            Timer.State.RUNNING -> listOf(pauseAction)
            Timer.State.PAUSED -> listOf(resumeAction, resetAction)
            else -> emptyList()
        }

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val contentTitle = context.getString(R.string.app_duration_hours_seconds_format, minutes, seconds)
        val contentText = when (type) {
            SessionType.WORK -> R.string.notifications_work_session
            else -> R.string.notifications_break
        }.let { stringId ->
            val text = context.getString(stringId)
            if (timerState == Timer.State.PAUSED) context.getString(R.string.notifications_paused_format, text) else text
        }

        return newPomodoroNotificationBuilder()
                .setContentTitle(contentTitle)
                .setContentText(contentText).also { builder ->
                    actions.forEach { builder.addAction(it) }
                }.build()
    }

    private fun newPomodoroNotificationBuilder(): NotificationCompat.Builder {
        val intent = Intent(context, HostActivity::class.java)
        val tapActionIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(context, TIMER_NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_app_icon)
                .setColor(context.getColorCompat(R.color.colorPrimary)) // TODO: resolve the color according to the app theme
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setChannelId(TIMER_NOTIFICATION_CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(tapActionIntent)
                .setAllowSystemGeneratedContextualActions(false)
                .setNotificationSilent()
    }

    private fun newAction(action: NotificationAction, iconRes: Int, title: String): NotificationCompat.Action {
        val intent = Intent(action.value)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(iconRes, title, pendingIntent)
    }

    companion object {
        const val TIMER_NOTIFICATION_ID = 1
        const val TIMER_NOTIFICATION_CHANNEL_ID = "TIMER"
    }
}