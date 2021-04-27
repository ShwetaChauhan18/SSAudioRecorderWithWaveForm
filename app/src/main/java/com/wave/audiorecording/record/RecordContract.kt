package com.wave.audiorecording.record

import android.content.Context
import android.icu.text.AlphabeticIndex
import android.net.Uri
import com.wave.audiorecording.audioplayer.recorder.RecorderContract
import com.wave.audiorecording.model.RecordInfo
import com.wave.audiorecording.util.Contract
import com.wave.audiorecording.util.IntArrayList
import java.io.File

interface RecordContract {
    interface View : Contract.View {
        fun keepScreenOn(on: Boolean)
        fun showRecordingStart()
        fun showRecordingStop()
        fun showRecordingPause()
        fun onRecordingProgress(mills: Long, amp: Int)
        fun askRecordingNewName(id: Long, file: File)
        fun startRecordingService()
        fun stopRecordingService()
        fun startPlaybackService(name: String?)
        fun showPlayStart(animate: Boolean)
        fun showPlayPause()
        fun showPlayStop()
        fun onPlayProgress(mills: Long, px: Int, percent: Int)
        fun showImportStart()
        fun hideImportProgress()
        fun showRecordProcessing()
        fun hideRecordProcessing()
        fun showWaveForm(waveForm: IntArray, duration: Long)
        fun waveFormToStart()
        fun showDuration(duration: String?)
        fun showRecordingProgress(progress: String?)
        fun showName(name: String?)
        fun askDeleteRecord(name: String?)
        fun askDeleteRecordForever()
        fun deleteRecord()
        fun showOptionsMenu()
        fun hideOptionsMenu()
        fun showRecordInfo(info: RecordInfo?)
        fun showRecordFile(file: File)
        fun updateRecordingView(data: IntArrayList?)
        fun showRecordsLostMessage(list: List<AlphabeticIndex.Record<*>?>?)
    }

    interface UserActionsListener : Contract.UserActionsListener<View?> {
        fun executeFirstRun()
        fun setAudioRecorder(recorder: RecorderContract.Recorder?)
        fun startRecording(context: Context?)
        fun pauseRecording(context: Context?)
        fun stopRecording(deleteRecord: Boolean)
        fun cancelRecording(context: Context?)
        fun startPlayback()
        fun pausePlayback()
        fun seekPlayback(px: Int)
        fun stopPlayback()
        fun renameRecord(id: Long, name: String?)
        fun loadActiveRecord()
        fun clearDatabase(): Boolean
        fun dontAskRename()
        fun importAudioFile(context: Context, uri: Uri)
        fun updateRecordingDir(context: Context?)
        fun setStoragePrivate(context: Context?)

        //TODO: Remove this getters
        val isStorePublic: Boolean
        val activeRecordPath: String?
        val activeRecordName: String?
        val activeRecordFullName: String?
        val activeRecordId: Int
        fun deleteActiveRecord(forever: Boolean)
        fun disablePlaybackProgressListener()
        fun enablePlaybackProgressListener()
    }
}