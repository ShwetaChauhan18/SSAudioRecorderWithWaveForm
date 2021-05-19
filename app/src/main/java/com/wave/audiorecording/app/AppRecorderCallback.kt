package com.wave.audiorecording.app

import com.wave.audiorecording.data.database.Record
import com.wave.audiorecording.exception.AppException
import java.io.File

interface AppRecorderCallback {
    fun onRecordingStarted(file: File?)
    fun onRecordingPaused()
    fun onRecordProcessing()
    fun onRecordFinishProcessing()
    fun onRecordingStopped(file: File?, record: Record?)
    fun onRecordingProgress(mills: Long, amp: Int)
    fun onError(throwable: AppException?)
}