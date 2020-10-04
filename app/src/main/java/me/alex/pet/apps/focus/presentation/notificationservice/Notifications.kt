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
import me.alex.pet.apps.focus.presentation.notificationservice.NotificationAction.*


class Notifications(private val context: Context) {

    private val pauseAction = PAUSE.toNotificationCompatAction(context)
    private val resumeAction = RESUME.toNotificationCompatAction(context)
    private val switchToWorkSessionAction = SWITCH_TO_WORK_SESSION.toNotificationCompatAction(context)
    private val switchToBreakAction = SWITCH_TO_BREAK.toNotificationCompatAction(context)
    private val resetAction = RESET.toNotificationCompatAction(context)

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
        return newPomodoroNotificationBuilder()
                .setContentTitle(context.getString(R.string.notifications_break_is_over))
                .setContentText(context.getString(R.string.app_ready_to_start_a_work_session))
                .addAction(switchToWorkSessionAction)
                .addAction(resetAction)
                .build()
    }

    fun newBreakIntroNotification(): Notification {
        return newPomodoroNotificationBuilder()
                .setContentTitle(context.getString(R.string.notifications_work_session_is_finished))
                .setContentText(context.getString(R.string.app_ready_to_take_a_break))
                .addAction(switchToBreakAction)
                .addAction(resetAction)
                .build()
    }

    fun newTimerNotification(type: SessionType, timerState: Timer.State, remainingSeconds: Long): Notification {
        val actions = when (timerState) {
            Timer.State.RUNNING -> listOf(pauseAction)
            Timer.State.PAUSED -> listOf(resumeAction, resetAction)
            else -> emptyList()
        }

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val contentTitle = context.getString(R.string.app_duration_minutes_seconds_format, minutes, seconds)
        val contentText = when (type) {
            SessionType.WORK -> R.string.notifications_work_session
            else -> R.string.notifications_break
        }.let { stringId ->
            val sessionDescription = context.getString(stringId)
            if (timerState == Timer.State.PAUSED) {
                context.getString(R.string.notifications_paused_format, sessionDescription)
            } else {
                sessionDescription
            }
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

    companion object {
        const val TIMER_NOTIFICATION_ID = 1
        const val TIMER_NOTIFICATION_CHANNEL_ID = "TIMER"
    }
}

private fun NotificationAction.toNotificationCompatAction(context: Context): NotificationCompat.Action {
    val intent = Intent(value)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    return NotificationCompat.Action(
            iconRes,
            context.getString(titleRes),
            pendingIntent
    )
}