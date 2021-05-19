package com.wave.audiorecording.data.database

import java.io.IOException

interface LocalRepository {
    fun open()
    fun close()
    fun getRecord(id: Int): Record?
    val allRecords: List<Record?>?
    fun getRecords(page: Int): List<Record?>?
    fun getRecords(page: Int, order: Int): List<Record?>?
    fun deleteAllRecords(): Boolean
    val lastRecord: Record?
    fun clearDatabase(): Boolean
    fun insertRecord(record: Record): Record?
    fun updateRecord(record: Record): Boolean

    @Throws(IOException::class)
    fun insertEmptyFile(filePath: String?): Record?
    fun deleteRecord(id: Int)
    fun deleteRecordForever(id: Int)
    val recordsDurations: List<Long?>?
    fun setOnRecordsLostListener(listener: OnRecordsLostListener?)
}