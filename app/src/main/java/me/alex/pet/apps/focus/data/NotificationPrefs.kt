package me.alex.pet.apps.focus.data

import android.content.Context
import android.net.Uri
import android.provider.Settings
import com.chibatching.kotpref.KotprefModel
import me.alex.pet.apps.focus.R

class NotificationPrefs(context: Context) : KotprefModel(context) {

    override val kotprefName: String = context.getString(R.string.prefs_app)

    val soundIsEnabled: Boolean by booleanPref(defaultSoundIsEnabled, R.string.pref_sound_on_off)

    val soundUri: Uri
        get() = Uri.parse(soundUriString)
    private val soundUriString: String by stringPref(defaultNotificationSoundUri.toString(), R.string.pref_sound)

    val vibrationIsEnabled: Boolean by booleanPref(defaultVibrationIsEnabled, R.string.pref_vibration_on_off)

    val vibrationPattern: LongArray = defaultVibrationPattern

    val soundChannel: NotificationSoundPlayer.SoundChannel = defaultSoundChannel


    companion object {
        const val defaultSoundIsEnabled: Boolean = true

        val defaultNotificationSoundUri: Uri = Settings.System.DEFAULT_NOTIFICATION_URI

        const val defaultVibrationIsEnabled: Boolean = true

        val defaultVibrationPattern: LongArray = longArrayOf(0L, 250L, 250L, 250L, 250L)

        val defaultSoundChannel: NotificationSoundPlayer.SoundChannel = NotificationSoundPlayer.SoundChannel.NOTIFICATION
    }
}