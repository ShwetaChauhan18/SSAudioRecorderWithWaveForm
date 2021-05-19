package com.wave.audiorecording.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * SQLite database manager class.
 */
class SQLiteHelper internal constructor(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_RECORDS_TABLE_SCRIPT)
        db.execSQL(CREATE_TRASH_TABLE_SCRIPT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(SQLiteHelper::class.java.name,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data")
        if (newVersion == 1) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS)
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRASH)
            onCreate(db)
        } else if (newVersion == 2) {
            db.execSQL(CREATE_TRASH_TABLE_SCRIPT)
        }
    }

    companion object {
        //Tables names
        const val TABLE_RECORDS = "records"
        const val TABLE_TRASH = "trash"

        //Fields for table Records
        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_DURATION = "duration"
        const val COLUMN_CREATION_DATE = "created"
        const val COLUMN_DATE_ADDED = "added"
        const val COLUMN_DATE_REMOVED = "removed"
        const val COLUMN_PATH = "path"

        /**
         * Simplified array of audio record amplitudes that represents waveform.
         */
        const val COLUMN_DATA = "data"
        const val COLUMN_DATA_STR = "data_str"
        const val COLUMN_WAVEFORM_PROCESSED = "waveform_processed"
        const val COLUMN_BOOKMARK = "bookmark"
        private const val DATABASE_NAME = "records.db"
        private const val DATABASE_VERSION = 2

        //Create records table sql statement
        private const val CREATE_RECORDS_TABLE_SCRIPT = ("CREATE TABLE " + TABLE_RECORDS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_DURATION + " LONG NOT NULL, "
                + COLUMN_CREATION_DATE + " LONG NOT NULL, "
                + COLUMN_DATE_ADDED + " LONG NOT NULL, "
                + COLUMN_PATH + " TEXT NOT NULL, "
                + COLUMN_DATA + " BLOB NOT NULL, "
                + COLUMN_BOOKMARK + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_WAVEFORM_PROCESSED + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_DATA_STR + " BLOB NOT NULL);")

        //Create trash table sql statement
        private const val CREATE_TRASH_TABLE_SCRIPT = ("CREATE TABLE " + TABLE_TRASH + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_DURATION + " LONG NOT NULL, "
                + COLUMN_CREATION_DATE + " LONG NOT NULL, "
                + COLUMN_DATE_ADDED + " LONG NOT NULL, "
                + COLUMN_DATE_REMOVED + " LONG NOT NULL, "
                + COLUMN_PATH + " TEXT NOT NULL, "
                + COLUMN_DATA + " BLOB NOT NULL, "
                + COLUMN_BOOKMARK + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_WAVEFORM_PROCESSED + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_DATA_STR + " BLOB NOT NULL);")
    }
}