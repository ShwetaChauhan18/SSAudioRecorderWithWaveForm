package com.wave.audiorecording.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log

/**
 * Class to communicate with table: [SQLiteHelper.TABLE_RECORDS] in database.
 */
class RecordsDataSource private constructor(context: Context) : DataSource<Record>(context, SQLiteHelper.Companion.TABLE_RECORDS) {
    override fun itemToContentValues(item: Record): ContentValues {
        return run {
            val values = ContentValues()
            if (item.id != Record.Companion.NO_ID) {
                values.put(SQLiteHelper.Companion.COLUMN_ID, item.id)
            }
            values.put(SQLiteHelper.Companion.COLUMN_NAME, item.getName())
            values.put(SQLiteHelper.Companion.COLUMN_DURATION, item.duration)
            values.put(SQLiteHelper.Companion.COLUMN_CREATION_DATE, item.created)
            values.put(SQLiteHelper.Companion.COLUMN_DATE_ADDED, item.added)
            values.put(SQLiteHelper.Companion.COLUMN_PATH, item.path)
            values.put(SQLiteHelper.Companion.COLUMN_BOOKMARK, if (item.isBookmarked) 1 else 0)
            values.put(SQLiteHelper.Companion.COLUMN_WAVEFORM_PROCESSED, if (item.isWaveformProcessed) 1 else 0)
            values.put(SQLiteHelper.Companion.COLUMN_DATA, item.data)
            //TODO: Remove this field from database.
            values.put(SQLiteHelper.Companion.COLUMN_DATA_STR, "")
            values
        }
    }

    override fun recordToItem(cursor: Cursor): Record {
        return Record(
                cursor.getInt(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_ID)),
                cursor.getString(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_NAME)),
                cursor.getLong(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_DURATION)),
                cursor.getLong(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_CREATION_DATE)),
                cursor.getLong(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_DATE_ADDED)),
                0,  //Record removed date not needed here.
                cursor.getString(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_PATH)),
                cursor.getInt(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_BOOKMARK)) != 0,
                cursor.getInt(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_WAVEFORM_PROCESSED)) != 0,
                cursor.getBlob(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_DATA))
        )
    }

    companion object {
        @Volatile
        private lateinit var instance: RecordsDataSource
        @JvmStatic
        fun getInstance(context: Context): RecordsDataSource {
            if (::instance.isInitialized.not()) {
                synchronized(RecordsDataSource::class.java) {
                    if (::instance.isInitialized.not()) {
                        instance = RecordsDataSource(context)
                    }
                }
            }
            return instance
        }
    }
}