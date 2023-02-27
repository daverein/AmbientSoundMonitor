package com.programmersbox.forestwoodass.anmonitor.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.programmersbox.forestwoodass.anmonitor.data.repository.SamplingSoundDataRepository
import java.util.*

class MuteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = SamplingSoundDataRepository(context)
        val launchCode = intent.getIntExtra(Notification.EXTRA_NOTIFICATION_ID, 0)
        Log.d("MuteNotificationReceiver", "Mute the notification ${launchCode}")
        repo.muteNotificationsUntil(Calendar.getInstance().timeInMillis + (60 * 1000 * launchCode))

        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.cancel(MY_NOTIFICATION_ID)
        return
    }
}
