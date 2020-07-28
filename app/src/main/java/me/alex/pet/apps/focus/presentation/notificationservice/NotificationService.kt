package me.alex.pet.apps.focus.presentation.notificationservice

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.SessionType
import me.alex.pet.apps.focus.domain.TimerState
import org.koin.android.ext.android.inject
import timber.log.Timber

class NotificationService : Service() {

    private val pomodoro by inject<Pomodoro>()

    private val pomodoroObserver = object : Pomodoro.Observer {
        override fun onUpdate() {
            Timber.d("onUpdate() ${pomodoro.remainingSeconds}")
            if (pomodoro.isReset) {
                stopForeground(true)
                stopSelf()
            } else {
                updateNotification(pomodoro.toNotification())
            }
        }
    }

    private val Pomodoro.isReset get() = timerState == TimerState.READY && completedWorkSessionCount == 0

    private val notificationActionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.retrieveAction()
            Timber.d("onReceive() action=$action")
            when (action) {
                NotificationAction.PAUSE, NotificationAction.RESUME -> pomodoro.toggleSession()
                NotificationAction.SWITCH_TO_NEXT_SESSION -> {
                    pomodoro.apply {
                        switchToNextSession()
                        startSession()
                    }
                }
                NotificationAction.RESET -> pomodoro.reset()
            }
        }

        private fun Intent.retrieveAction(): NotificationAction {
            return NotificationAction.from(this.action)
        }
    }

    private val notifications = Notifications(this)

    override fun onCreate() {
        Timber.d("onCreate()")
        val intentFilter = IntentFilter().apply {
            NotificationAction.values().forEach { action -> addAction(action.value) }
        }
        registerReceiver(notificationActionsReceiver, intentFilter)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand()")
        startForeground(Notifications.TIMER_NOTIFICATION_ID, pomodoro.toNotification())
        subscribeToModel()
        return START_NOT_STICKY
    }

    private fun subscribeToModel() {
        pomodoro.addObserver(pomodoroObserver)
    }

    private fun updateNotification(notification: Notification) {
        NotificationManagerCompat.from(this).notify(Notifications.TIMER_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        pomodoro.removeObserver(pomodoroObserver)
        unregisterReceiver(notificationActionsReceiver)
    }

    private fun Pomodoro.toNotification(): Notification {
        return when {
            isAwaitingSessionSwitch -> {
                if (nextSessionType == SessionType.WORK) {
                    notifications.newWorkIntroNotification()
                } else {
                    notifications.newBreakIntroNotification()
                }
            }
            else -> notifications.newTimerNotification(sessionType, timerState, remainingSeconds)
        }
    }
}