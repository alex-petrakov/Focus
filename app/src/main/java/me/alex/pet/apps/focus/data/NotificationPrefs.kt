package me.alex.pet.apps.focus.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

class NotificationPrefs(context: Context) {

    // TODO: (1) Implement actual preferences
    val soundIsEnabled: Boolean = true

    val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val vibrationIsEnabled: Boolean = true

    val vibrationPattern: LongArray = longArrayOf(0L, 250L, 250L, 250L, 250L)

    val soundChannel: NotificationSoundPlayer.SoundChannel = NotificationSoundPlayer.SoundChannel.ALARM
}