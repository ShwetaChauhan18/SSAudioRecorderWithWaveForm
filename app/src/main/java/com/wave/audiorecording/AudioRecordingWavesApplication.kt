package com.wave.audiorecording

import android.app.Application
import android.os.Handler
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import com.wave.audiorecording.util.AndroidUtils.getScreenWidth
import com.wave.audiorecording.util.AndroidUtils.pxToDp
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.DeviceUtils.isTabletDevice
import com.wave.audiorecording.util.Injector

class AudioRecordingWavesApplication : Application(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "${BuildConfig.VERSION_NAME} |${BuildConfig.VERSION_CODE}|")
        // test
        Log.i(TAG, "Initializing " + (if (isTabletDevice(this)) "tablet" else "phone") + " version")
        androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
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
        private val TAG = AudioRecordingWavesApplication::class.java.simpleName

        @Volatile
        var applicationHandler: Handler? = null
        var injector: Injector? = null
        var androidId: String? = null
            private set

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