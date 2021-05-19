package com.wave.audiorecording.app

import com.wave.audiorecording.audioplayer.recorder.RecorderContract
import com.wave.audiorecording.data.database.Record
import com.wave.audiorecording.util.IntArrayList

interface AppRecorder {
    fun addRecordingCallback(recorderCallback: AppRecorderCallback?)
    fun removeRecordingCallback(recorderCallback: AppRecorderCallback?)
    fun setRecorder(recorder: RecorderContract.Recorder)
    fun startRecording(filePath: String?, channelCount: Int, sampleRate: Int, bitrate: Int)
    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording()
    fun decodeRecordWaveform(decRec: Record)
    val recordingData: IntArrayList
    val recordingDuration: Long
    val isRecording: Boolean
    val isPaused: Boolean
    fun release()
    val isProcessing: Boolean
}