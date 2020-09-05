package me.alex.pet.apps.focus.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import timber.log.Timber
import java.io.IOException

class NotificationSoundPlayer(
        private val context: Context,
        private val notificationSettings: NotificationPrefs
) {

    fun play() {
        if (notificationSettings.soundIsEnabled) {
            playSound()
        }
        if (notificationSettings.vibrationIsEnabled) {
            vibrate()
        }
    }

    private fun playSound() {
        MediaPlayer().apply {
            try {
                setDataSource(context, notificationSettings.soundUri)
                setAudioAttributes(notificationSettings.soundChannel.audioAttributes)
                setOnPreparedListener(::onMediaPlayerPrepared)
                prepareAsync()
            } catch (e: IOException) {
                Timber.w(e)
            } catch (e: IllegalArgumentException) {
                Timber.w(e)
            }
        }
    }

    private fun onMediaPlayerPrepared(player: MediaPlayer) {
        player.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    private fun vibrate() {
        obtainVibrator()?.vibrateOnce(notificationSettings.vibrationPattern)
    }

    private fun obtainVibrator(): Vibrator? {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        return if (vibrator == null || !vibrator.hasVibrator()) {
            Timber.w("Device doesn't have a vibrator")
            null
        } else {
            vibrator
        }
    }


    enum class SoundChannel(val id: Int, val audioAttributes: AudioAttributes) {
        ALARM(
                0,
                AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
        ),
        NOTIFICATION(
                1,
                AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
        );

        companion object {
            fun from(id: Int): SoundChannel {
                return when (id) {
                    0 -> ALARM
                    1 -> NOTIFICATION
                    else -> throw IllegalArgumentException("Unknown sound channel id: $id")
                }
            }
        }
    }
}


private fun Vibrator.vibrateOnce(vibrationPattern: LongArray) {
    val disableRepeating = -1
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, disableRepeating)
        vibrate(vibrationEffect)
    } else {
        @Suppress("DEPRECATION")
        vibrate(vibrationPattern, disableRepeating)
    }
}