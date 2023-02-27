package com.programmersbox.forestwoodass.anmonitor.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.programmersbox.forestwoodass.anmonitor.utils.MonitorDBLevels
import com.programmersbox.forestwoodass.anmonitor.utils.SoundRecorder
import java.time.Duration
import kotlinx.coroutines.*


class SamplingService : Service() {
    private lateinit var monitorDB: MonitorDBLevels
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var soundRecorder: SoundRecorder
    private val blockingObject = Object()
    private lateinit var runningService: Job
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onCreate() {
        super.onCreate()
        keyguardManager =getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        monitorDB = MonitorDBLevels(this)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Channel human readable title",
            NotificationManager.IMPORTANCE_MIN)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("title")
            .setContentText("text").build()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    private fun isCallActive(context: Context): Boolean {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return manager.mode == AudioManager.MODE_IN_CALL
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!::runningService.isInitialized) {
            runningService = serviceScope.launch {
                runMonitoringAlgorithm()
            }
        } else {
            synchronized(blockingObject) {
                blockingObject.notify()
            }
        }
        return START_STICKY
    }

    private suspend fun runMonitoringAlgorithm() {
        // Make sure the service is properly shutdown from another instance before starting
        delay(10*1000)
        val intent = Intent(baseContext, SamplingService::class.java)
        val pendingIntent = PendingIntent.getService(baseContext, 0,
                                intent, PendingIntent.FLAG_IMMUTABLE)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
       // We may want to use this instead: am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 1000*60*5, pendingIntent)


        while (runningService.isActive) {
            if (!::soundRecorder.isInitialized) {
                try {
                    soundRecorder = SoundRecorder(baseContext)
                } catch (ex: java.lang.UnsupportedOperationException) {
                    // Probably a permission issue
                }
            }
            if (::soundRecorder.isInitialized && !isCallActive(baseContext)
                && !keyguardManager.isDeviceLocked) {
                startSample()
                monitorDB.recordDBLevel(soundRecorder.maxDB.toFloat())
            } else {
                monitorDB.reset()
            }
            Log.d(TAG,"Waiting ${1000*60*monitorDB.getNextSampleDelay()}ms before taking another sample")

            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime()+1000*60*monitorDB.getNextSampleDelay(),
                pendingIntent)
            synchronized(blockingObject) {
                blockingObject.wait()
            }
        }
        if (::soundRecorder.isInitialized ) {
            soundRecorder.release()
        }

        // Cancel the alarms so the service doesn't start again
        val cancelPendingIntent = PendingIntent.getService(baseContext, 0,
            intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(cancelPendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy, cancelling service")
        if (::runningService.isInitialized) {
            runningService.cancel()
        }
    }

    private suspend fun startSample() {
        Log.d(TAG, "Getting a sound-recorder instance")
        coroutineScope {
            // Kick off a parallel job to record
            val recordingJob = launch {

                Log.d(TAG, "checking permissions")
                if (ActivityCompat.checkSelfPermission(
                        baseContext,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }

                Log.d(TAG, "Starting recording")
                soundRecorder.record()
            }
            val maxRecordingDuration: Duration = Duration.ofSeconds(5)
            val delayPerTickMs = maxRecordingDuration.toMillis() / 5
            val startTime = System.currentTimeMillis()

            repeat(5) { index ->
                delay(startTime + delayPerTickMs * (index + 1) - System.currentTimeMillis())
                // This could be a chance to send status to the UI, but the Tile will only update
                // once every 20 seconds, so not doing anything now.
            }
            // Stop recording
            Log.d(TAG, "stopping recording")
            recordingJob.cancel()

        }
    }

    companion object {
        private const val TAG = "SamplingService"
        private const val CHANNEL_ID = "my_channel_01"
    }
}
