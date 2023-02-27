package com.programmersbox.forestwoodass.anmonitor.utils

import android.app.*
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.programmersbox.forestwoodass.anmonitor.R
import com.programmersbox.forestwoodass.anmonitor.presentation.DBMonitor


const val MY_NOTIFICATION_ID = 101

class NotificationHelper(val context: Context) {
    private val metrics: DisplayMetrics = context.resources.displayMetrics
    private val screenWidth: Int = metrics.widthPixels

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager?
        val mChannel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        mChannel.description = channelDescription
        notificationManager!!.createNotificationChannel(mChannel)
    }

    private fun generateBitmapImage(maxdb: Float): Bitmap? {
        val dbString = String.format("%.1f", maxdb)
        val textPainter = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val textPainter2 = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val strokeWidth = 17f
        val edgeOffset = 14f
        val ballRadius = 13f
        val textOffset: Float
        val padding = 10f

       // textPainter.typeface = context.resources.getFont(R.font.rubik_medium)
        textPainter.textSize = 64f
        textPainter.color = Color.White.toArgb()
        val rect = Rect()
        textPainter.getTextBounds(dbString, 0, dbString.length, rect)

        val rect2 = Rect()
        textPainter2.textSize = 24f
        textPainter2.color = Color.White.toArgb()
        textPainter2.getTextBounds(dbString, 0, dbString.length, rect2)

        val bitmap = Bitmap.createBitmap(
            screenWidth,
            rect.height() + (edgeOffset.toInt()) + strokeWidth.toInt() + padding.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        textOffset = screenWidth/2f - rect.width()/2f - rect2.width()/2f

        canvas.drawText(
            dbString, 0f + textOffset, 0f + rect.height(),
            textPainter
        )
        canvas.drawText(
            "db", textOffset + rect.width().toFloat() + 5f, 0f + rect.height(),
            textPainter2
        )

        val a = intArrayOf(Color.Green.toArgb(), Color.Yellow.toArgb(), Color.Red.toArgb())
        val b = floatArrayOf(0.0f, 0.8f, 0.95f)
        val shader: Shader = LinearGradient(
            0f,
            0f,
            screenWidth.toFloat() - (edgeOffset * 2),
            strokeWidth,
            a,
            b,
            Shader.TileMode.CLAMP
        )
        val paint = Paint()
        paint.shader = shader
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(
            edgeOffset, edgeOffset + rect.height() + padding,
            screenWidth - (edgeOffset * 2), edgeOffset + rect.height() + padding, paint
        )

        paint.color = Color.White.toArgb()
        paint.shader = null
        paint.style = Paint.Style.FILL
        canvas.drawCircle(
            edgeOffset + ((screenWidth - edgeOffset * 2) * (maxdb / 100f)),
            edgeOffset + rect.height() + padding,
            ballRadius,
            paint
        )

        return bitmap
    }

    fun showNotification(maxdb: Float, timeInLoudEnvMS: Long) {
        val notificationManager =
            context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager?
        val intent = Intent(context, DBMonitor::class.java)
        val pIntent = PendingIntent.getActivity(
            context, 0,
            intent, PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = Intent(context, MuteNotificationReceiver::class.java)
        snoozeIntent.action = "com.programmersbox.forestwoodass.wearable.watchface.MUTE"
        snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, 60)
        val snoozePendingIntent =
            PendingIntent.getBroadcast(context, 60, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)
        val snoozeIntent2 = Intent(context, MuteNotificationReceiver::class.java)
        snoozeIntent2.action = "com.programmersbox.forestwoodass.wearable.watchface.MUTE"
        snoozeIntent2.putExtra(EXTRA_NOTIFICATION_ID, 120)
        val snoozePendingIntent2 =
            PendingIntent.getBroadcast(context, 120, snoozeIntent2, PendingIntent.FLAG_IMMUTABLE)

        val dbString = String.format("%.1f", maxdb)

        val bitmap = generateBitmapImage(maxdb)

        val body = String.format(
            context.getString(R.string.sampling_notification_body),
            dbString,
            timeInLoudEnvMS / (1000 * 60)
        )
        val action: NotificationCompat.Action =
            NotificationCompat.Action(
                R.drawable.hearing_damage_small,
                context.getString(R.string.snooze),
                snoozePendingIntent
            )
        val action2: NotificationCompat.Action =
            NotificationCompat.Action(
                R.drawable.hearing_damage_small,
                context.getString(R.string.snooze2),
                snoozePendingIntent2
            )
        val icon = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.hearing_damage_small
        )
        val smallBitmap = Bitmap.createScaledBitmap(bitmap!!, bitmap.width*4, bitmap.height*4, true)
        val myStyle = NotificationCompat.BigPictureStyle()
            .setBigContentTitle(context.getString(R.string.sampling_notification_expanded_title))
            .setSummaryText(context.getString(R.string.sampling_notification_expanded_summary))
            .bigPicture(smallBitmap!!)
            .bigLargeIcon(icon)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
           myStyle.showBigPictureWhenCollapsed(false)
        }


        val n: Notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.sampling_notification_title))
            .setContentText(body)
            .setChannelId(channelId)
            .setContentIntent(pIntent)
            .setStyle(myStyle)
          //  .setLargeIcon(smallBitmap)
            .addAction(action)
            .addAction(action2)
            .setSmallIcon(R.drawable.hearing_damage_small).build()

        Log.d(TAG, "Sending notification")
        notificationManager!!.notify(MY_NOTIFICATION_ID, n)
    }

    companion object {
        private const val TAG = "NotificationHelper"
        private const val channelName = "Sound Level Warning"
        private const val channelDescription = "hearing damage"
        private const val channelId = "SoundLevelWarning"
    }
}
