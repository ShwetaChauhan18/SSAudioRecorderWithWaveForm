package com.wave.audiorecording.data

import android.content.Context
import com.wave.audiorecording.exception.CantCreateFileException
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.FileUtil
import java.io.File
import java.io.FileNotFoundException

class FileRepositoryImpl private constructor(context: Context, prefs: Prefs) : FileRepository {
    override var recordingDir: File? = null
        private set
    private val prefs: Prefs

    @Throws(CantCreateFileException::class)
    override fun provideRecordFile(): File {
        prefs.incrementRecordCounter()
        val recordFile: File?
        val recordName: String = if (prefs.namingFormat == AppConstants.NAMING_COUNTED) {
            FileUtil.generateRecordNameCounted(prefs.recordCounter)
        } else {
            FileUtil.generateRecordNameDate()
        }
        recordFile = if (prefs.format == AppConstants.RECORDING_FORMAT_WAV) {
            FileUtil.createFile(recordingDir, FileUtil.addExtension(recordName, AppConstants.WAV_EXTENSION))
        } else {
            FileUtil.createFile(recordingDir, FileUtil.addExtension(recordName, AppConstants.M4A_EXTENSION))
        }
        if (recordFile != null) {
            return recordFile
        }
        throw CantCreateFileException()
    }

    @Throws(CantCreateFileException::class)
    override fun provideRecordFile(name: String?): File {
        val recordFile = FileUtil.createFile(recordingDir, name)
        if (recordFile != null) {
            return recordFile
        }
        throw CantCreateFileException()
    }

    override fun deleteRecordFile(path: String?): Boolean {
        return if (path != null) {
            FileUtil.deleteFile(File(path))
        } else false
    }

    override fun markAsTrashRecord(path: String?): String? {
        val trashLocation = path?.let { FileUtil.addExtension(it, AppConstants.TRASH_MARK_EXTENSION) }
        return if (FileUtil.renameFile(File(path), File(trashLocation))) {
            trashLocation
        } else null
    }

    override fun unmarkTrashRecord(path: String?): String? {
        val restoredFile = path?.let { FileUtil.removeFileExtension(it) }
        return if (FileUtil.renameFile(File(path), File(restoredFile))) {
            restoredFile
        } else null
    }

    override fun deleteAllRecords(): Boolean {
        return recordingDir?.let { FileUtil.deleteFile(it) } == true
    }

    override fun renameFile(path: String?, newName: String?, extension: String?): Boolean {
        return newName?.let { extension?.let { it1 -> FileUtil.renameFile(File(path), it, it1) } } == true
    }

    override fun updateRecordingDir(context: Context, prefs: Prefs) {
        if (prefs.isStoreDirPublic) {
            recordingDir = FileUtil.getAppDir(context)
            if (recordingDir == null) {
                //Try to init private dir
                try {
                    recordingDir = FileUtil.getPrivateRecordsDir(context)
                } catch (e: FileNotFoundException) {
                    //Timber.e(e);
                    //If nothing helped then hardcode recording dir
                    recordingDir = File("/data/data/" + context.packageName + "/files")
                }
            }
        } else {
            try {
                recordingDir = FileUtil.getPrivateRecordsDir(context)
            } catch (e: FileNotFoundException) {
                //Timber.e(e);
                //Try to init public dir
                recordingDir = FileUtil.getAppDir(context)
                if (recordingDir == null) {
                    //If nothing helped then hardcode recording dir
                    recordingDir = File("/data/data/" + context.packageName + "/files")
                }
            }
        }
    }

    override fun hasAvailableSpace(context: Context?): Boolean {
        var space: Long = 0
        var time: Long = 0
        context?.let {
            space = if (prefs.isStoreDirPublic) {
                FileUtil.getAvailableExternalMemorySize(context)
            } else {
                FileUtil.getAvailableInternalMemorySize(context)
            }
            time = spaceToTimeSecs(space, prefs.format, prefs.sampleRate, prefs.recordChannelCount)

        }
        return time > AppConstants.MIN_REMAIN_RECORDING_TIME
    }

    private fun spaceToTimeSecs(spaceBytes: Long, format: Int, sampleRate: Int, channels: Int): Long {
        return if (format == AppConstants.RECORDING_FORMAT_M4A) {
            1000 * (spaceBytes / (AppConstants.RECORD_ENCODING_BITRATE_48000 / 8))
        } else if (format == AppConstants.RECORDING_FORMAT_WAV) {
            1000 * (spaceBytes / (sampleRate * channels * 2))
        } else {
            0
        }
    }

    companion object {
        @Volatile
        private lateinit var instance: FileRepositoryImpl

        @JvmStatic
        fun getInstance(context: Context, prefs: Prefs): FileRepositoryImpl {
            if (!::instance.isInitialized) {
                synchronized(FileRepositoryImpl::class.java) {
                    if (!::instance.isInitialized) {
                        instance = FileRepositoryImpl(context, prefs)
                    }
                }
            }
            return instance
        }
    }

    init {
        updateRecordingDir(context, prefs)
        this.prefs = prefs
    }
}