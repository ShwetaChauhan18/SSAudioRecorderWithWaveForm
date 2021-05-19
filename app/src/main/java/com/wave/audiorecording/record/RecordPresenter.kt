package com.wave.audiorecording.record

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.wave.audiorecording.BackgroundQueue
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.R
import com.wave.audiorecording.app.AppRecorder
import com.wave.audiorecording.app.AppRecorderCallback
import com.wave.audiorecording.audioplayer.recorder.RecorderContract
import com.wave.audiorecording.data.FileRepository
import com.wave.audiorecording.data.Prefs
import com.wave.audiorecording.data.database.LocalRepository
import com.wave.audiorecording.data.database.OnRecordsLostListener
import com.wave.audiorecording.data.database.Record
import com.wave.audiorecording.exception.AppException
import com.wave.audiorecording.exception.CantCreateFileException
import com.wave.audiorecording.exception.ErrorParser
import com.wave.audiorecording.player.PlayerContract.Player
import com.wave.audiorecording.player.PlayerContract.PlayerCallback
import com.wave.audiorecording.util.AndroidUtils
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.FileUtil
import com.wave.audiorecording.util.TimeUtils
import java.io.File
import java.io.IOException
import java.util.Date

class RecordPresenter(private val prefs: Prefs, private val fileRepository: FileRepository,
                      private val localRepository: LocalRepository,
                      private val audioPlayer: Player,
                      private val appRecorder: AppRecorder,
                      private val recordingsTasks: BackgroundQueue,
                      private val loadingTasks: BackgroundQueue,
                      private val importTasks: BackgroundQueue) : RecordContract.UserActionsListener {
    private var view: RecordContract.View? = null
    private var playerCallback: PlayerCallback? = null
    private var appRecorderCallback: AppRecorderCallback? = null
    private lateinit var copyTasks: BackgroundQueue
    private var songDuration: Long = 0
    private var dpPerSecond = AppConstants.SHORT_RECORD_DP_PER_SECOND.toFloat()
    private var record: Record? = null
    private var deleteRecord = false
    private var listenPlaybackProgress = true

    /**
     * Flag true defines that presenter called to show import progress when view was not bind.
     * And after view bind we need to show import progress.
     */
    private var showImportProgress = false
    override fun bindView(v: RecordContract.View?) {
        view = v
        if (showImportProgress) {
            view?.showImportStart()
        } else {
            view?.hideImportProgress()
        }
        if (!prefs.hasAskToRenameAfterStopRecordingSetting()) {
            prefs.isAskToRenameAfterStopRecording = true
        }
        if (appRecorderCallback == null) {
            appRecorderCallback = object : AppRecorderCallback {
                override fun onRecordingStarted(file: File?) {
                    if (view != null) {
                        view?.showRecordingStart()
                        view?.keepScreenOn(prefs.isKeepScreenOn)
                        view?.startRecordingService()
                    }
                }

                override fun onRecordingPaused() {
                    if (view != null) {
                        view?.keepScreenOn(false)
                        view?.showRecordingPause()
                    }
                    if (deleteRecord) {
                        if (view != null) {
                            deleteRecord = false
                        }
                    }
                }

                override fun onRecordProcessing() {
                    if (view != null) {
                        view?.showRecordProcessing()
                    }
                }

                override fun onRecordFinishProcessing() {
                    if (view != null) {
                        view?.hideRecordProcessing()
                    }
                    loadActiveRecord()
                }

                override fun onRecordingStopped(file: File?, rec: Record?) {
                    if (deleteRecord) {
                        deleteActiveRecord(true)
                        deleteRecord = false
                    } else {
                        rec?.let {
                            file?.let { refFile ->
                                if (view != null) {
                                    if (prefs.isAskToRenameAfterStopRecording) {
                                        view?.askRecordingNewName(it.id.toLong(), refFile)
                                    }
                                }
                                record = it
                                songDuration = it.duration
                                dpPerSecond = AudioRecordingWavesApplication.getDpPerSecond(songDuration.toFloat() / 1000000f)
                                if (view != null) {
                                    view?.showWaveForm(it.amps, songDuration)
                                    view?.showName(FileUtil.removeFileExtension(it.getName()))
                                    view?.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(songDuration / 1000))
                                    view?.showRecordFile(refFile)
                                }
                            }

                        }

                    }
                    if (view != null) {
                        view?.keepScreenOn(false)
                        view?.stopRecordingService()
                        view?.hideProgress()
                        view?.showRecordingStop()
                    }
                }

                override fun onRecordingProgress(mills: Long, amp: Int) {
                    AndroidUtils.runOnUIThread( {
                        if (view != null) {
                            view?.onRecordingProgress(mills, amp)
                        }
                    })
                }

                override fun onError(throwable: AppException?) {
                    if (view != null) {
                        view?.showError(ErrorParser.parseException(throwable))
                        view?.showRecordingStop()
                    }
                }
            }
        }
        appRecorder.addRecordingCallback(appRecorderCallback)
        if (playerCallback == null) {
            playerCallback = object : PlayerCallback {
                override fun onPreparePlay() {
                    if (record != null) {
                        view?.startPlaybackService(record?.getName())
                    }
                }

                override fun onStartPlay() {
                    if (view != null) {
                        view?.showPlayStart(true)
                    }
                }

                override fun onPlayProgress(mills: Long) {
                    if (view != null && listenPlaybackProgress) {
                        AndroidUtils.runOnUIThread( {
                            if (view != null) {
                                val duration = songDuration / 1000
                                if (duration > 0) {
                                    view?.onPlayProgress(mills, AndroidUtils.convertMillsToPx(mills,
                                            AndroidUtils.dpToPx(dpPerSecond)), (1000 * mills / duration).toInt())
                                }
                            }
                        })
                    }
                }

                override fun onStopPlay() {
                    if (view != null) {
                        audioPlayer.seek(0)
                        view?.showPlayStop()
                        view?.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(songDuration / 1000))
                    }
                }

                override fun onPausePlay() {
                    if (view != null) {
                        view?.showPlayPause()
                    }
                }

                override fun onSeek(mills: Long) {}
                override fun onError(throwable: AppException?) {
                    if (view != null) {
                        view?.showError(ErrorParser.parseException(throwable))
                    }
                }
            }
        }
        audioPlayer.addPlayerCallback(playerCallback)
        if (audioPlayer.isPlaying) {
            view?.showPlayStart(false)
        } else if (audioPlayer.isPause) {
            if (view != null) {
                val duration = songDuration / 1000
                if (duration > 0) {
                    val playProgressMills = audioPlayer.pauseTime
                    view?.onPlayProgress(playProgressMills, AndroidUtils.convertMillsToPx(playProgressMills,
                            AndroidUtils.dpToPx(dpPerSecond)), (1000 * playProgressMills / duration).toInt())
                }
                view?.showPlayPause()
            }
        } else {
            audioPlayer.seek(0)
            view?.showPlayStop()
        }
        if (appRecorder.isPaused) {
            view?.keepScreenOn(false)
            view?.showRecordingPause()
            view?.showRecordingProgress(TimeUtils.formatTimeIntervalHourMinSec2(appRecorder.recordingDuration))
            view?.updateRecordingView(appRecorder.recordingData)
        } else if (appRecorder.isRecording) {
            view?.showRecordingStart()
            view?.showRecordingProgress(TimeUtils.formatTimeIntervalHourMinSec2(appRecorder.recordingDuration))
            view?.keepScreenOn(prefs.isKeepScreenOn)
            view?.updateRecordingView(appRecorder.recordingData)
        } else {
            view?.showRecordingStop()
            view?.keepScreenOn(false)
        }
        if (appRecorder.isProcessing) {
            view?.showRecordProcessing()
        } else {
            view?.hideRecordProcessing()
        }
        localRepository.setOnRecordsLostListener(object : OnRecordsLostListener {
            override fun onLostRecords(list: List<Record?>?) {
                // lost record file
            }
        })
    }

    override fun unbindView() {
        if (view != null) {
            audioPlayer.removePlayerCallback(playerCallback)
            appRecorder.removeRecordingCallback(appRecorderCallback)
            localRepository.setOnRecordsLostListener(null)
            view = null
        }
    }

    override fun clear() {
        if (view != null) {
            unbindView()
        }
        localRepository.close()
        audioPlayer.release()
        appRecorder.release()
        loadingTasks.close()
        recordingsTasks.close()
    }

    override fun executeFirstRun() {
        if (prefs.isFirstRun) {
            prefs.firstRunExecuted()
        }
    }

    override fun setAudioRecorder(recorder: RecorderContract.Recorder?) {
        recorder?.let { appRecorder.setRecorder(it) }
    }

    override fun startRecording(context: Context?) {
        if (fileRepository.hasAvailableSpace(context)) {
            if (audioPlayer.isPlaying) {
                audioPlayer.stop()
            }
            if (appRecorder.isPaused) {
                appRecorder.resumeRecording()
            } else if (!appRecorder.isRecording) {
                try {
                    val path = fileRepository.provideRecordFile().absolutePath
                    recordingsTasks.postRunnable( {
                        try {
                            record = localRepository.insertEmptyFile(path)
                            record?.let {
                                prefs.activeRecord = it.id.toLong()
                                AndroidUtils.runOnUIThread( {
                                    appRecorder.startRecording(
                                            path,
                                            prefs.recordChannelCount,
                                            prefs.sampleRate,
                                            prefs.bitrate
                                    )
                                })
                            }

                        } catch (e: IOException) {
                            Log.e("TAG", e.message.toString())
                        } catch (e: OutOfMemoryError) {
                            Log.e("TAG", e.message.toString())
                        } catch (e: IllegalStateException) {
                            Log.e("TAG", e.message.toString())
                        }
                    })
                } catch (e: CantCreateFileException) {
                    if (view != null) {
                        view?.showError(ErrorParser.parseException(e))
                    }
                }
            } else {
                appRecorder.pauseRecording()
            }
        } else {
            view?.showError(R.string.error_no_available_space)
        }
    }

    override fun pauseRecording(context: Context?) {
        if (fileRepository.hasAvailableSpace(context)) {
            if (audioPlayer.isPlaying) {
                appRecorder.pauseRecording()
            }
        }
    }

    override fun stopRecording(delete: Boolean) {
        if (appRecorder.isRecording) {
            deleteRecord = delete
            if (view != null) {
                view?.showProgress()
                view?.waveFormToStart()
            }
            audioPlayer.seek(0)
            appRecorder.stopRecording()
        }
    }

    override fun cancelRecording(context: Context?) {
        deleteRecord = true
        appRecorder.pauseRecording()
    }

    override fun startPlayback() {
        record?.let {
            if (!audioPlayer.isPlaying) {
                audioPlayer.setData(it.path)
            }
            audioPlayer.playOrPause()
        }
    }

    override fun pausePlayback() {
        if (audioPlayer.isPlaying) {
            audioPlayer.pause()
        }
    }

    override fun seekPlayback(px: Int) {
        audioPlayer.seek(AndroidUtils.convertPxToMills(px.toLong(), AndroidUtils.dpToPx(dpPerSecond)).toLong())
    }

    override fun stopPlayback() {
        audioPlayer.stop()
    }

    override fun renameRecord(id: Long, n: String?) {
        if (id < 0 || n == null || n.isEmpty()) {
            AndroidUtils.runOnUIThread( {
                if (view != null) {
                    view?.showError(R.string.error_failed_to_rename)
                }
            })
            return
        }
        if (view != null) {
            view?.showProgress()
        }
        val name = FileUtil.removeUnallowedSignsFromName(n)
        loadingTasks.postRunnable ({
            val recordDbItem = localRepository.getRecord(id.toInt())
            if (recordDbItem != null) {
                val nameWithExt: String
                nameWithExt = if (prefs.format == AppConstants.RECORDING_FORMAT_WAV) {
                    name + AppConstants.EXTENSION_SEPARATOR + AppConstants.WAV_EXTENSION
                } else {
                    name + AppConstants.EXTENSION_SEPARATOR + AppConstants.M4A_EXTENSION
                }
                val file = File(recordDbItem.path)
                val renamed = File(file.parentFile.absolutePath + File.separator + nameWithExt)
                if (renamed.exists()) {
                    AndroidUtils.runOnUIThread( {
                        if (view != null) {
                            view?.showError(R.string.error_file_exists)
                        }
                    })
                } else {
                    val ext: String
                    ext = if (prefs.format == AppConstants.RECORDING_FORMAT_WAV) {
                        AppConstants.WAV_EXTENSION
                    } else {
                        AppConstants.M4A_EXTENSION
                    }
                    if (fileRepository.renameFile(recordDbItem.path, name, ext)) {
                        record = Record(recordDbItem.id, nameWithExt, recordDbItem.duration, recordDbItem.created,
                                recordDbItem.added, recordDbItem.removed, renamed.absolutePath, recordDbItem.isBookmarked,
                                recordDbItem.isWaveformProcessed, recordDbItem.amps)
                        record?.let {
                            if (localRepository.updateRecord(it)) {
                                AndroidUtils.runOnUIThread( {
                                    if (view != null) {
                                        view?.hideProgress()
                                        view?.showName(name)
                                    }
                                })
                            } else {
                                AndroidUtils.runOnUIThread( { view?.showError(R.string.error_failed_to_rename) })
                                //Restore file name after fail update path in local database.
                                if (renamed.exists()) {
                                    //Try to rename 3 times;
                                    if (!renamed.renameTo(file)) {
                                        if (!renamed.renameTo(file)) {
                                            renamed.renameTo(file)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        AndroidUtils.runOnUIThread( {
                            if (view != null) {
                                view?.showError(R.string.error_failed_to_rename)
                            }
                        })
                    }
                }
                AndroidUtils.runOnUIThread( {
                    if (view != null) {
                        view?.hideProgress()
                    }
                })
            }
        })
    }

    override fun loadActiveRecord() {
        if (!appRecorder.isRecording) {
            view?.showProgress()
            loadingTasks.postRunnable( {
                val rec = localRepository.getRecord(prefs.activeRecord.toInt())
                record = rec
                if (rec != null) {
                    songDuration = rec.duration
                    dpPerSecond = AudioRecordingWavesApplication.getDpPerSecond(songDuration.toFloat() / 1000000f)
                    AndroidUtils.runOnUIThread( {
                        if (view != null) {
                            view?.showWaveForm(rec.amps, songDuration)
                            view?.showName(FileUtil.removeFileExtension(rec.getName()))
                            view?.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(songDuration / 1000))
                            view?.showOptionsMenu()
                            view?.hideProgress()
                        }
                    })
                } else {
                    AndroidUtils.runOnUIThread( {
                        if (view != null) {
                            view?.hideProgress()
                            view?.showWaveForm(intArrayOf(), 0)
                            view?.showName("")
                            view?.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(0))
                            view?.deleteRecord()
                            view?.hideOptionsMenu()
                        }
                    })
                }
            })
        }
    }

    override fun clearDatabase(): Boolean {
        localRepository.clearDatabase()
        return true
    }

    override fun dontAskRename() {
        prefs.isAskToRenameAfterStopRecording = false
    }

    override fun updateRecordingDir(context: Context?) {
        context?.let { fileRepository.updateRecordingDir(it, prefs) }
    }

    override fun setStoragePrivate(context: Context?) {
        prefs.isStoreDirPublic = false
        context?.let { fileRepository.updateRecordingDir(it, prefs) }
    }

    override val isStorePublic: Boolean
        get() = prefs.isStoreDirPublic
    override val activeRecordPath: String?
        get() = if (record != null) {
            record?.path
        } else {
            null
        }
    override val activeRecordName: String?
        get() = if (record != null) {
            record?.getName()?.let { FileUtil.removeFileExtension(it) }
        } else {
            null
        }
    override val activeRecordFullName: String?
        get() = if (record != null) {
            record?.getName()
        } else {
            null
        }
    override val activeRecordId: Int
        get() = record?.id ?: run {
            -1
        }

    override fun deleteActiveRecord(forever: Boolean) {
        val rec = record
        if (rec != null) {
            audioPlayer.stop()
            recordingsTasks.postRunnable( {
                if (forever) {
                    localRepository.deleteRecordForever(rec.id)
                    fileRepository.deleteRecordFile(rec.path)
                } else {
                    localRepository.deleteRecord(rec.id)
                }
                prefs.activeRecord = -1
                dpPerSecond = AppConstants.SHORT_RECORD_DP_PER_SECOND.toFloat()
                AndroidUtils.runOnUIThread( {
                    if (view != null) {
                        view?.showWaveForm(intArrayOf(), 0)
                        view?.showName("")
                        view?.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(0))
                        if (!forever) {
                            view?.showMessage(R.string.record_moved_into_trash)
                        }
                        view?.onPlayProgress(0, 0, 0)
                        view?.hideProgress()
                        record = null
                    }
                })
            })
        }
    }

    override fun disablePlaybackProgressListener() {
        listenPlaybackProgress = false
    }

    override fun enablePlaybackProgressListener() {
        listenPlaybackProgress = true
    }

//    override fun bindView(view: RecordContract.View?) {
//        TODO("Not yet implemented")
//    }

    override fun importAudioFile(context: Context, uri: Uri) {
        if (view != null) {
            view?.showImportStart()
        }
        showImportProgress = true
        importTasks.postRunnable(object : Runnable {
            var id: Long = -1
            override fun run() {
                try {
                    val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    val fileDescriptor = parcelFileDescriptor?.fileDescriptor
                    val name = extractFileName(context, uri)
                    val newFile = fileRepository.provideRecordFile(name)
                    if (FileUtil.copyFile(fileDescriptor, newFile)) {
                        val duration = AndroidUtils.readRecordDuration(newFile)
                        //Do 2 step import: 1) Import record with empty waveform. 2) Process and update waveform in background.
                        val r = Record(
                                Record.NO_ID,
                                newFile.name,
                                duration,
                                newFile.lastModified(),
                                Date().time,
                                0,
                                newFile.absolutePath,
                                false,
                                false, IntArray(AudioRecordingWavesApplication.longWaveformSampleCount))
                        record = localRepository.insertRecord(r)
                        val rec = record
                        if (rec != null) {
                            id = rec.id.toLong()
                            prefs.activeRecord = id
                            songDuration = duration
                            dpPerSecond = AudioRecordingWavesApplication.getDpPerSecond(songDuration.toFloat() / 1000000f)
                            AndroidUtils.runOnUIThread( {
                                if (view != null) {
                                    audioPlayer.stop()
                                    view?.showWaveForm(rec.amps, songDuration)
                                    view?.showName(FileUtil.removeFileExtension(rec.getName()))
                                    view?.showDuration(TimeUtils.formatTimeIntervalHourMinSec2(songDuration / 1000))
                                    view?.hideProgress()
                                    view?.hideImportProgress()
                                    //view.showOptionsMenu();
                                }
                            })
                            appRecorder.decodeRecordWaveform(rec)
                        }
                    }
                } catch (e: SecurityException) {
                    AndroidUtils.runOnUIThread( { if (view != null) view?.showError(R.string.error_permission_denied) })
                } catch (e: IOException) {
                    AndroidUtils.runOnUIThread( { if (view != null) view?.showError(R.string.error_unable_to_read_sound_file) })
                } catch (e: OutOfMemoryError) {
                    AndroidUtils.runOnUIThread( { if (view != null) view?.showError(R.string.error_unable_to_read_sound_file) })
                } catch (e: IllegalStateException) {
                    AndroidUtils.runOnUIThread( { if (view != null) view?.showError(R.string.error_unable_to_read_sound_file) })
                } catch (ex: CantCreateFileException) {
                    AndroidUtils.runOnUIThread( { if (view != null) view?.showError(ErrorParser.parseException(ex)) })
                }
                AndroidUtils.runOnUIThread( {
                    if (view != null) {
                        view?.hideImportProgress()
                    }
                })
                showImportProgress = false
            }
        })
    }

    private fun extractFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                //				TODO: find a better way to extract file extension.
                return if (!name.contains(".")) {
                    "$name.m4a"
                } else name
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    init {
        AudioRecordingWavesApplication.injector?.provideCopyTasksQueue()?.let {
            copyTasks = it
        }
    }
}