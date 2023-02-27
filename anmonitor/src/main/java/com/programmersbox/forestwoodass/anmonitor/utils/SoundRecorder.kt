/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.programmersbox.forestwoodass.anmonitor.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC
import android.media.AudioManager.GET_DEVICES_INPUTS
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext


private const val DEFAULT_LOW_END_DB = -90.0

/**
 * A helper class to provide methods to record audio input from the MIC to the internal storage
 * and to playback the same recorded audio file.
 */
@SuppressLint("MissingPermission")
class SoundRecorder(var context: Context) {
    private var state = State.IDLE

    var liveMaxDB = 0.0
    var liveMinDB = 0.0
    var liveAvgDB = 0.0
    var maxDB = -96.0

    private enum class State {
        IDLE, RECORDING
    }

    private var audioRecord: AudioRecord = AudioRecord.Builder()
        .setAudioSource(MediaRecorder.AudioSource.MIC)
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(RECORDING_RATE)
                .setChannelMask(CHANNEL_IN)
                .setEncoding(FORMAT)
                .build()
        )
        .setBufferSizeInBytes(AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT) * 3)
        .build()

    init {
        Log.d(TAG, "On Device ${Build.HARDWARE} / ${Build.ID} / ${Build.MODEL} / ${Build.MANUFACTURER}")
        // From Watch5
        //On Device s5e5515 / RWS3.220419.001 / SM-R905U / samsung
        //builtin 11 and bottom
        //MIC 14   '0' 25 -3.4028235E38  -3.4028235E38  -3.4028235E38 14 0
        with(context.getSystemService(Context.AUDIO_SERVICE) as AudioManager) {
            val device = getDevices(GET_DEVICES_INPUTS).first { it.type == TYPE_BUILTIN_MIC }
            Log.d(TAG, "builtin ${device.id} and ${device.address}")
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mics = manager.microphones

            for( mic in mics ) {
                //if ( mic.id == id ) {
                Log.d(TAG, "MIC ${mic.id}   '${mic.address}' ${mic.type} ${mic.sensitivity}  ${mic.maxSpl}  ${mic.minSpl} ${mic.description} ${mic.directionality}")
                //micSPLmax = mic.maxSpl.toDouble()
                // }
            }
        }
    }
    /**
     * Records from the microphone.
     *
     * This method is cancellable, and cancelling it will stop recording.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun record() {
        if (state != State.IDLE) {
            Log.w(TAG, "Requesting to start recording while state was not IDLE")
            return
        }

        state = State.RECORDING

        val intSize = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT)

        audioRecord.startRecording()

        try {
            withContext(Dispatchers.IO) {
                val buffer = ByteArray(intSize)
                var numSamples = 0
                var totalMaxValue = -128.0
                var totalMinValue = 0.0
                var totalAvgValue = 0.0
                var totalRunMaxValue = 0.0
                while (isActive) {
                    if ( audioRecord.read(buffer, 0, buffer.size) < buffer.size ) {
                        break
                    }

                    val shorts = buffer
                        .asList()
                        .chunked(2)
                        .map { (l, h) -> (l.toInt() or h.toInt().shl(8)).toShort() }
                        .toShortArray()
                    var runMin = 0.0
                    var runMax = DEFAULT_LOW_END_DB
                    var runAvg = 0.0
                    var numProcessed = 0
                    shorts.forEach {
                        val rawDBFS = 20.0 * log10(abs(it.toDouble()) / 32768.0)

                        val valueDBFS = max(rawDBFS, DEFAULT_LOW_END_DB)

                        if ( valueDBFS > DEFAULT_LOW_END_DB ) {
                            runMin = min(runMin, valueDBFS)
                            runMax = max(runMax, valueDBFS)
                            runAvg += valueDBFS
                            numProcessed++
                        }


                    }
                    runAvg /= numProcessed
                    if ( runMin == DEFAULT_LOW_END_DB && runMax == DEFAULT_LOW_END_DB) {
                        Log.d(TAG, "Startup values.  ignore...")
                        runMin = 0.0
                        continue
                    }

                    Log.d(TAG, "short scan run: $runMin $runMax $runAvg")

                    numSamples++

                    totalMaxValue = max(totalMaxValue, runMax)
                    totalMinValue = min(totalMinValue, runAvg)
                    totalAvgValue += runMax

                    val baseDB = abs(DEFAULT_LOW_END_DB)
                    liveMinDB = baseDB + runAvg
                    liveMaxDB = baseDB + totalMaxValue
                    liveAvgDB = baseDB + runMax

                    Log.d(TAG, "min $liveMinDB and max $liveMaxDB and avg $liveAvgDB")
                    totalRunMaxValue += (baseDB + runMax)
                }
                audioRecord.stop()

                maxDB = totalRunMaxValue/numSamples

                Log.d(TAG, "audioRecord $maxDB")
            }
        } finally {
            state = State.IDLE
            Log.d(TAG, "audioRecord = released")
        }
    }

    fun release() {
        audioRecord.release()
    }

    companion object {
        private const val TAG = "SoundRecorder"
        private const val RECORDING_RATE = 8000 // can go up to 44K, if needed
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}
