package com.wave.audiorecording.data.database

import android.database.SQLException
import android.util.Log
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.data.FileRepository
import com.wave.audiorecording.util.AppConstants
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Date

class LocalRepositoryImpl private constructor(private val dataSource: RecordsDataSource, private val fileRepository: FileRepository) : LocalRepository {
    private var onLostRecordsListener: OnRecordsLostListener? = null
    override fun open() {
        dataSource.open()
    }

    override fun close() {
        dataSource.close()
    }

    override fun getRecord(id: Int): Record? {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        val r = dataSource.getItem(id)
        if (r != null) {
            val l: MutableList<Record?> = ArrayList(1)
            l.add(r)
            checkForLostRecords(l)
            return r
        }
        return null
    }

    override fun insertRecord(record: Record): Record? {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        return dataSource.insertItem(record)
    }

    override fun updateRecord(record: Record): Boolean {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        //If updated record count is more than 0, then update is successful.
        return dataSource.updateItem(record) > 0
    }

    @Throws(IOException::class)
    override fun insertEmptyFile(path: String?): Record? {
        if (path != null && !path.isEmpty()) {
            val file = File(path)
            val record = Record(
                    Record.Companion.NO_ID,
                    file.name,
                    0,  //mills
                    file.lastModified(),
                    Date().time,
                    0,
                    path,
                    false,
                    false, IntArray(AudioRecordingWavesApplication.longWaveformSampleCount))
            val r = insertRecord(record)
            if (r != null) {
                return r
            } else {
                Log.e("", "Failed to insert record into local database!")
            }
        } else {
            Log.e("LocalRepositoryImpl", "Unable to read sound file by specified path!")
            throw IOException("Unable to read sound file by specified path!")
        }
        return null
    }

    override val allRecords: List<Record?>?
        get() {
            if (!dataSource.isOpen) {
                dataSource.open()
            }
            val list: List<Record?>? = dataSource.all
            checkForLostRecords(list)
            return list
        }

    override fun getRecords(page: Int): List<Record?>? {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        val list: List<Record?>? = dataSource.getRecords(page)
        checkForLostRecords(list)
        return list
    }

    override fun getRecords(page: Int, order: Int): List<Record?>? {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        val orderStr: String
        orderStr = when (order) {
            AppConstants.SORT_NAME -> SQLiteHelper.Companion.COLUMN_NAME + " ASC"
            AppConstants.SORT_NAME_DESC -> SQLiteHelper.Companion.COLUMN_NAME + " DESC"
            AppConstants.SORT_DURATION -> SQLiteHelper.Companion.COLUMN_DURATION + " DESC"
            AppConstants.SORT_DURATION_DESC -> SQLiteHelper.Companion.COLUMN_DURATION + " ASC"
            AppConstants.SORT_DATE_DESC -> SQLiteHelper.Companion.COLUMN_DATE_ADDED + " ASC"
            AppConstants.SORT_DATE -> SQLiteHelper.Companion.COLUMN_DATE_ADDED + " DESC"
            else -> SQLiteHelper.Companion.COLUMN_DATE_ADDED + " DESC"
        }
        val list: List<Record?>? = dataSource.getRecords(page, orderStr)
        checkForLostRecords(list)
        return list
    }

    //If Audio file deleted then delete record from local database.
    override val lastRecord: Record?
        get() {
            if (!dataSource.isOpen) {
                dataSource.open()
            }
            val c = dataSource.queryLocal("SELECT * FROM " + SQLiteHelper.Companion.TABLE_RECORDS +
                    " ORDER BY " + SQLiteHelper.Companion.COLUMN_ID + " DESC LIMIT 1")
            return if (c != null && c.moveToFirst()) {
                val r = dataSource.recordToItem(c)
                if (isFileExists(r.path)) {
                    r
                } else {
                    //If Audio file deleted then delete record from local database.
                    val l: MutableList<Record?> = ArrayList(1)
                    l.add(r)
                    checkForLostRecords(l)
                    r
                }
            } else {
                null
            }
        }

    override fun clearDatabase(): Boolean {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        dataSource.deleteAll()
        return true
    }

    private fun isFileExists(path: String?): Boolean {
        return File(path).exists()
    }

    override fun deleteRecord(id: Int) {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        val recordToDelete = dataSource.getItem(id)
        if (recordToDelete != null) {
            val renamed = fileRepository.markAsTrashRecord(recordToDelete.path)
            if (renamed != null) {
                recordToDelete.path = renamed
            }
        }
        dataSource.deleteItem(id)
    }

    override fun deleteRecordForever(id: Int) {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        dataSource.deleteItem(id)
    }

    override val recordsDurations: List<Long?>?
        get() {
            if (!dataSource.isOpen) {
                dataSource.open()
            }
            return dataSource.recordsDurations
        }

    override fun deleteAllRecords(): Boolean {
        if (!dataSource.isOpen) {
            dataSource.open()
        }
        return try {
            true
        } catch (e: SQLException) {
            false
        }
    }

    private fun checkForLostRecords(list: List<Record?>?) {
        val lost: MutableList<Record?> = ArrayList()
        list?.let {
            for (i in it.indices) {
                if (!isFileExists(it[i]?.path)) {
                    lost.add(it[i])
                }
            }
        }
        if (onLostRecordsListener != null && !lost.isEmpty()) {
            onLostRecordsListener?.onLostRecords(lost)
        }
    }

    override fun setOnRecordsLostListener(onLostRecordsListener: OnRecordsLostListener?) {
        this.onLostRecordsListener = onLostRecordsListener
    }

    companion object {
        @Volatile
        private lateinit var instance: LocalRepositoryImpl
        @JvmStatic
        fun getInstance(source: RecordsDataSource, fileRepository: FileRepository): LocalRepositoryImpl {
            if (!::instance.isInitialized) {
                synchronized(LocalRepositoryImpl::class.java) {
                    if (!::instance.isInitialized) {
                        instance = LocalRepositoryImpl(source, fileRepository)
                    }
                }
            }
            return instance
        }
    }
}