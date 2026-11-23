package com.andymatt1.radionebulosa

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class RadioNebulosaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createPlaybackChannel()
    }

    private fun createPlaybackChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción de Radio Nebulosa"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "radio_nebulosa_playback"
    }
}
