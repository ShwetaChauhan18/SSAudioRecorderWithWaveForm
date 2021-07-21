package com.wave.audiorecording.util

import android.content.Context
import com.wave.audiorecording.BackgroundQueue
import com.wave.audiorecording.app.AppRecorder
import com.wave.audiorecording.app.AppRecorderImpl.Companion.getInstance
import com.wave.audiorecording.audioplayer.recorder.AudioRecorder
import com.wave.audiorecording.audioplayer.recorder.RecorderContract
import com.wave.audiorecording.audioplayer.recorder.WavRecorder
import com.wave.audiorecording.data.FileRepository
import com.wave.audiorecording.data.FileRepositoryImpl.Companion.getInstance
import com.wave.audiorecording.data.Prefs
import com.wave.audiorecording.data.PrefsImpl
import com.wave.audiorecording.data.database.LocalRepository
import com.wave.audiorecording.data.database.LocalRepositoryImpl.Companion.getInstance
import com.wave.audiorecording.data.database.RecordsDataSource
import com.wave.audiorecording.player.AudioPlayer
import com.wave.audiorecording.player.PlayerContract.Player
import com.wave.audiorecording.record.RecordContract
import com.wave.audiorecording.record.RecordPresenter

class Injector(private val context: Context) {
    private var loadingTasks: BackgroundQueue? = null
    private var recordingTasks: BackgroundQueue? = null
    private var importTasks: BackgroundQueue? = null
    private var processingTasks: BackgroundQueue? = null
    private var copyTasks: BackgroundQueue? = null
    private var mainPresenter: RecordContract.UserActionsListener? = null
    fun providePrefs(): Prefs {
        return PrefsImpl.getInstance(context)
    }

    fun provideRecordsDataSource(): RecordsDataSource {
        return RecordsDataSource.getInstance(context)
    }

    fun provideFileRepository(): FileRepository {
        return getInstance(context, providePrefs())
    }

    fun provideLocalRepository(): LocalRepository {
//        var mLocalRepository: LocalRepository? = null
//        provideRecordsDataSource()?.let { recordData ->
//            provideFileRepository()?.let { fileRepo ->
//                {
//                    mLocalRepository = getInstance(recordData, fileRepo)
//                }
//            }
//        }
        return getInstance(provideRecordsDataSource(), provideFileRepository())
    }

    fun provideAppRecorder(): AppRecorder {
        return getInstance(provideAudioRecorder(), provideLocalRepository(),
                provideLoadingTasksQueue(), provideProcessingTasksQueue(), providePrefs())
    }

    fun provideLoadingTasksQueue(): BackgroundQueue {
        if (loadingTasks == null) {
            loadingTasks = BackgroundQueue("LoadingTasks")
        }
        return loadingTasks as BackgroundQueue
    }

    fun provideRecordingTasksQueue(): BackgroundQueue {
        if (recordingTasks == null) {
            recordingTasks = BackgroundQueue("RecordingTasks")
        }
        return recordingTasks as BackgroundQueue
    }

    fun provideImportTasksQueue(): BackgroundQueue {
        if (importTasks == null) {
            importTasks = BackgroundQueue("ImportTasks")
        }
        return importTasks as BackgroundQueue
    }

    fun provideProcessingTasksQueue(): BackgroundQueue {
        if (processingTasks == null) {
            processingTasks = BackgroundQueue("ProcessingTasks")
        }
        return processingTasks as BackgroundQueue
    }

    fun provideCopyTasksQueue(): BackgroundQueue {
        if (copyTasks == null) {
            copyTasks = BackgroundQueue("CopyTasks")
        }
        return copyTasks as BackgroundQueue
    }

    fun provideAudioPlayer(): Player {
        return AudioPlayer.instance
    }

    fun provideAudioRecorder(): RecorderContract.Recorder {
        return if (providePrefs()?.format == AppConstants.RECORDING_FORMAT_WAV) {
            WavRecorder.instance
        } else {
            AudioRecorder.instance
        }
    }

    fun provideMainPresenter(): RecordContract.UserActionsListener? {
        if (mainPresenter == null) {
            mainPresenter = RecordPresenter(providePrefs(), provideFileRepository(),
            provideLocalRepository(), provideAudioPlayer(), provideAppRecorder(),
            provideLoadingTasksQueue(), provideRecordingTasksQueue(), provideImportTasksQueue())
        }
        return mainPresenter
    }

    fun releaseMainPresenter() {
        if (mainPresenter != null) {
            mainPresenter?.clear()
            mainPresenter = null
        }
    }

    fun closeTasks() {
        loadingTasks?.cleanupQueue()
        loadingTasks?.close()
        importTasks?.cleanupQueue()
        importTasks?.close()
        processingTasks?.cleanupQueue()
        processingTasks?.close()
        recordingTasks?.cleanupQueue()
        recordingTasks?.close()
    }
}