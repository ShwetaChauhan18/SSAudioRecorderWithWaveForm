package com.wave.audiorecording.audioplayer.recorder

import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.audioplayer.recorder.RecorderContract.RecorderCallback
import com.wave.audiorecording.exception.InvalidOutputFile
import com.wave.audiorecording.exception.RecorderInitException
import com.wave.audiorecording.util.AppConstants
import java.io.File
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class AudioRecorder private constructor() : RecorderContract.Recorder {
    private var recorder: MediaRecorder? = null
    private var recordFile: File? = null
    private var isPrepared = false
    override var isRecording = false
        private set
    override var isPaused = false
        private set
    private var timerProgress: Timer? = null
    private var progress: Long = 0
    private var recorderCallback: RecorderCallback? = null
    override fun setRecorderCallback(callback: RecorderCallback?) {
        recorderCallback = callback
    }

    override fun prepare(outputFile: String?, channelCount: Int, sampleRate: Int, bitrate: Int) {
        recordFile = File(outputFile)
        if (recordFile?.exists() == true && recordFile?.isFile == true) {
            recorder = MediaRecorder()
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(channelCount)
                setAudioSamplingRate(sampleRate)
                setAudioEncodingBitRate(bitrate)
                setMaxDuration(-1) //Duration unlimited
                setOutputFile(recordFile?.absolutePath)
            }
            try {
                recorder?.prepare()
                isPrepared = true
                if (recorderCallback != null) {
                    recorderCallback?.onPrepareRecord()
                }
            } catch (e: IOException) {
                Log.e("AudioRecorder", "prepare() failed")
                if (recorderCallback != null) {
                    recorderCallback?.onError(RecorderInitException())
                }
            } catch (e: IllegalStateException) {
                Log.e("AudioRecorder", "prepare() failed")
                if (recorderCallback != null) {
                    recorderCallback?.onError(RecorderInitException())
                }
            }
        } else {
            if (recorderCallback != null) {
                recorderCallback?.onError(InvalidOutputFile())
            }
        }
    }

    override fun startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPaused) {
            try {
                recorder?.resume()
                startRecordingTimer()
                if (recorderCallback != null) {
                    recorderCallback?.onStartRecord(recordFile)
                }
                isPaused = false
            } catch (e: IllegalStateException) {
                Log.e("AudioRecorder", "unpauseRecording() failed")
                if (recorderCallback != null) {
                    recorderCallback?.onError(RecorderInitException())
                }
            }
        } else {
            if (isPrepared) {
                try {
                    recorder?.start()
                    isRecording = true
                    AudioRecordingWavesApplication.isRecording = (true)
                    startRecordingTimer()
                    if (recorderCallback != null) {
                        recorderCallback?.onStartRecord(recordFile)
                    }
                } catch (e: RuntimeException) {
                    Log.e("AudioRecorder", "startRecording() failed")
                    if (recorderCallback != null) {
                        recorderCallback?.onError(RecorderInitException())
                    }
                }
            } else {
                Log.e("AudioRecorder", "Recorder is not prepared?!")
            }
            isPaused = false
        }
    }

    override fun pauseRecording() {
        if (isRecording) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    recorder?.pause()
                    pauseRecordingTimer()
                    if (recorderCallback != null) {
                        recorderCallback?.onPauseRecord()
                    }
                    isPaused = true
                } catch (e: IllegalStateException) {
                    Log.e("AudioRecorder", "pauseRecording() failed")
                    if (recorderCallback != null) {
                        //TODO: Fix exception
                        recorderCallback?.onError(RecorderInitException())
                    }
                }
            } else {
                stopRecording()
            }
        }
    }

    override fun stopRecording() {
        if (isRecording) {
            stopRecordingTimer()
            try {
                recorder?.stop()
                AudioRecordingWavesApplication.isRecording = (false)
            } catch (e: RuntimeException) {
                Log.e("AudioRecorder", "stopRecording() problems")
            }
            recorder?.release()
            if (recorderCallback != null) {
                recorderCallback?.onStopRecord(recordFile)
            }
            recordFile = null
            isPrepared = false
            isRecording = false
            isPaused = false
            recorder = null
        } else {
            Log.e("AudioRecorder", "Recording has already stopped or hasn't started")
        }
    }

    private fun startRecordingTimer() {
        timerProgress = Timer()
        timerProgress?.schedule(object : TimerTask() {
            override fun run() {
                recorder?.let {
                    if (recorderCallback != null && recorder != null) {
                        try {
                            recorderCallback?.onRecordProgress(progress, it.maxAmplitude)
                        } catch (e: IllegalStateException) {
                            Log.e("AudioRecorder", e.message.toString())
                        }
                        progress += AppConstants.VISUALIZATION_INTERVAL
                    }
                }
            }
        }, 0, AppConstants.VISUALIZATION_INTERVAL)
    }

    private fun stopRecordingTimer() {
        timerProgress?.cancel()
        timerProgress?.purge()
        progress = 0
    }

    private fun pauseRecordingTimer() {
        timerProgress?.cancel()
        timerProgress?.purge()
    }

    private object RecorderSingletonHolder {
        val singleton = AudioRecorder()
    }

    companion object {
        @JvmStatic
        val instance: AudioRecorder
            get() = RecorderSingletonHolder.singleton
    }
}