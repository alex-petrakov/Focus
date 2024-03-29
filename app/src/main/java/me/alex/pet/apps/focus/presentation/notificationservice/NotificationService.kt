package me.alex.pet.apps.focus.presentation.notificationservice

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import me.alex.pet.apps.focus.data.NotificationSoundPlayer
import me.alex.pet.apps.focus.domain.Pomodoro
import me.alex.pet.apps.focus.domain.SessionType
import me.alex.pet.apps.focus.domain.Timer
import org.koin.android.ext.android.inject
import timber.log.Timber

class NotificationService : Service() {

    private val pomodoro by inject<Pomodoro>()

    private val notificationSoundPlayer by inject<NotificationSoundPlayer>()

    private val pomodoroObserver = object : Pomodoro.Observer {
        override fun onUpdate() {
            Timber.d("onUpdate() ${pomodoro.remainingDuration}")
            if (pomodoro.isReset) {
                stopForeground(true)
                stopSelf()
            } else {
                updateNotification(pomodoro.toNotification())
                if (pomodoro.isAwaitingSessionSwitch) {
                    notificationSoundPlayer.play()
                }
            }
        }
    }

    private val Pomodoro.isReset get() = timerState == Timer.State.READY && completedWorkSessionCount == 0

    private val notificationActionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.retrieveAction()
            Timber.d("onReceive() action=$action")
            when (action) {
                NotificationAction.PAUSE, NotificationAction.RESUME -> pomodoro.toggleSession()
                NotificationAction.SWITCH_TO_WORK_SESSION, NotificationAction.SWITCH_TO_BREAK -> pomodoro.startNextSession()
                NotificationAction.RESET -> pomodoro.reset()
            }
        }

        private fun Intent.retrieveAction(): NotificationAction {
            return NotificationAction.from(this.action)
        }
    }

    private lateinit var notifications: Notifications

    private var isRunning = false

    override fun onCreate() {
        Timber.d("onCreate()")
        notifications = Notifications(this)
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
        if (!isRunning) {
            startForeground(Notifications.TIMER_NOTIFICATION_ID, pomodoro.toNotification())
            subscribeToModel()
            isRunning = true
        }
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
            else -> notifications.newTimerNotification(sessionType, timerState, remainingDuration.seconds)
        }
    }
}