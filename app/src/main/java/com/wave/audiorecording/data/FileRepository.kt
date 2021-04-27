package com.wave.audiorecording.data

import android.content.Context
import com.wave.audiorecording.exception.CantCreateFileException
import java.io.File

interface FileRepository {
    @Throws(CantCreateFileException::class)
    fun provideRecordFile(): File

    @Throws(CantCreateFileException::class)
    fun provideRecordFile(name: String?): File

    //	File getRecordFileByName(String name, String extension);
    val recordingDir: File?
    fun deleteRecordFile(path: String?): Boolean
    fun markAsTrashRecord(path: String?): String?
    fun unmarkTrashRecord(path: String?): String?
    fun deleteAllRecords(): Boolean
    fun renameFile(path: String?, newName: String?, extension: String?): Boolean
    fun updateRecordingDir(context: Context, prefs: Prefs)
    fun hasAvailableSpace(context: Context?): Boolean
}