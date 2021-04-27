package com.wave.audiorecording.audioplayer.recorder

import com.wave.audiorecording.exception.AppException
import java.io.File

interface RecorderContract {
    interface RecorderCallback {
        fun onPrepareRecord()
        fun onStartRecord(output: File?)
        fun onPauseRecord()
        fun onRecordProgress(mills: Long, amp: Int)
        fun onStopRecord(output: File?)
        fun onError(throwable: AppException?)
    }

    interface Recorder {
        fun setRecorderCallback(callback: RecorderCallback?)
        fun prepare(outputFile: String?, channelCount: Int, sampleRate: Int, bitrate: Int)
        fun startRecording()
        fun pauseRecording()
        fun stopRecording()
        val isRecording: Boolean
        val isPaused: Boolean
    }
}