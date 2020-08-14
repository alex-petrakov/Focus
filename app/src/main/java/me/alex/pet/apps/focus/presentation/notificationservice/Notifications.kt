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
import me.alex.pet.apps.focus.domain.Session
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
                .setContentTitle("Break is over")
                .setContentText("Ready to start a new focus session?")
                .addAction(sessionSwitchAction)
                .addAction(resetAction)
                .build()
    }

    fun newBreakIntroNotification(): Notification {
        val resetAction = newAction(NotificationAction.RESET, R.drawable.ic_action_reset, context.getString(R.string.app_action_reset))
        val sessionSwitchAction = newAction(NotificationAction.SWITCH_TO_NEXT_SESSION, R.drawable.ic_action_switch_session, context.getString(R.string.app_start_break))
        return newPomodoroNotificationBuilder()
                .setContentTitle("Focus session is finished")
                .setContentText("Ready to take a break?")
                .addAction(sessionSwitchAction)
                .addAction(resetAction)
                .build()
    }

    fun newTimerNotification(type: Session.Type, timerState: Session.TimerState, remainingSeconds: Long): Notification {
        val pauseAction = newAction(NotificationAction.PAUSE, R.drawable.ic_action_pause, context.getString(R.string.app_action_pause))
        val resumeAction = newAction(NotificationAction.RESUME, R.drawable.ic_action_start, context.getString(R.string.app_action_resume))
        val resetAction = newAction(NotificationAction.RESET, R.drawable.ic_action_reset, context.getString(R.string.app_action_reset))
        return newPomodoroNotificationBuilder().apply {
            when (timerState) {
                Session.TimerState.RUNNING -> {
                    setContentTitle(remainingSeconds.toString())
                    setContentText(type.toString())
                    addAction(pauseAction)
                }
                Session.TimerState.PAUSED -> {
                    setContentTitle("$remainingSeconds (paused)")
                    setContentText(type.toString())
                    addAction(resumeAction)
                    addAction(resetAction)
                }
                else -> {
                    setContentTitle(remainingSeconds.toString())
                    setContentText(type.toString())
                }
            }
        }.build()
    }

    private fun newPomodoroNotificationBuilder(): NotificationCompat.Builder {
        val intent = Intent(context, HostActivity::class.java)
        val tapActionIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // TODO: Replace notification icon with a custom one once it's ready
        // TODO: Set notification color
        return NotificationCompat.Builder(context, TIMER_NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.ic_action_start)
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