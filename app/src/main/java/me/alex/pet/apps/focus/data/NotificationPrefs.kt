package me.alex.pet.apps.focus.data

import android.content.Context
import android.net.Uri
import android.provider.Settings
import com.chibatching.kotpref.KotprefModel
import me.alex.pet.apps.focus.R

class NotificationPrefs(context: Context) : KotprefModel(context) {

    override val kotprefName: String = context.getString(R.string.prefs_app)

    val soundIsEnabled: Boolean by booleanPref(DEFAULT_SOUND_IS_ENABLED, R.string.pref_sound_on_off)

    val soundUri: Uri
        get() = Uri.parse(soundUriString)
    private val soundUriString: String by stringPref(DEFAULT_NOTIFICATION_SOUND_URI.toString(), R.string.pref_sound)

    val vibrationIsEnabled: Boolean by booleanPref(DEFAULT_VIBRATION_IS_ENABLED, R.string.pref_vibration_on_off)

    val vibrationPattern: LongArray = DEFAULT_VIBRATION_PATTERN.toLongArray()

    val soundChannel: NotificationSoundPlayer.SoundChannel = DEFAULT_SOUND_CHANNEL


    companion object {
        const val DEFAULT_SOUND_IS_ENABLED: Boolean = true

        val DEFAULT_NOTIFICATION_SOUND_URI: Uri = Settings.System.DEFAULT_NOTIFICATION_URI

        const val DEFAULT_VIBRATION_IS_ENABLED: Boolean = true

        val DEFAULT_VIBRATION_PATTERN: List<Long> = listOf(0L, 250L, 250L, 250L, 250L)

        val DEFAULT_SOUND_CHANNEL: NotificationSoundPlayer.SoundChannel = NotificationSoundPlayer.SoundChannel.NOTIFICATION
    }
}