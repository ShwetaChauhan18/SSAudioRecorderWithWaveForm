package com.wave.audiorecording

import android.app.Application
import android.os.Handler
import androidx.lifecycle.LifecycleObserver
import com.wave.audiorecording.util.AndroidUtils.getScreenWidth
import com.wave.audiorecording.util.AndroidUtils.pxToDp
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.Injector

class AudioRecordingWavesApplication : Application(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()

        initializeAudio()
    }

    private fun initializeAudio() {
        applicationHandler = Handler(applicationContext.mainLooper)
        screenWidthDp = pxToDp(getScreenWidth(applicationContext))
        injector = Injector(applicationContext)
    }

    override fun onTerminate() {
        super.onTerminate()
        injector?.apply {
            releaseMainPresenter()
            closeTasks()

        }
    }

    companion object {

        @Volatile
        var applicationHandler: Handler? = null
        var injector: Injector? = null

        /**
         * Screen width in dp
         */
        private var screenWidthDp = 0f
        var isRecording = false

        /**
         * Calculate density pixels per second for record duration.
         * Used for visualisation waveform in view.
         *
         * @param durationSec record duration in seconds
         */
        fun getDpPerSecond(durationSec: Float): Float {
            return if (durationSec > AppConstants.LONG_RECORD_THRESHOLD_SECONDS) {
                AppConstants.WAVEFORM_WIDTH * screenWidthDp / durationSec
            } else {
                AppConstants.SHORT_RECORD_DP_PER_SECOND.toFloat()
            }
        }

        val longWaveformSampleCount: Int
            get() = (AppConstants.WAVEFORM_WIDTH * screenWidthDp).toInt()
    }
}