package com.wave.audiorecording.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.wave.audiorecording.BuildConfig
import com.wave.audiorecording.util.AppConstants
import java.util.ArrayList

/**
 * Base class to communicate with some table T in database.
 */
abstract class DataSource<T>(context: Context?, tableName: String) {
    /**
     * Tag for logging messages.
     */
    private val LOG_TAG = javaClass.simpleName

    /**
     * SQLite database manager.
     */
    protected var dbHelper: SQLiteHelper

    /**
     * Class provides access to database.
     */
    protected var db: SQLiteDatabase? = null

    /**
     * Source table name.
     */
    protected var tableName: String

    /**
     * Open connection to SQLite database.
     */
    fun open() {
        db = dbHelper.writableDatabase
    }

    /**
     * Close connection to SQLite database.
     */
    fun close() {
        db?.close()
        dbHelper.close()
    }

    val isOpen: Boolean
        get() = db != null && db?.isOpen == true

    /**
     * Insert new item into database for table T.
     *
     * @param item Item that will be inserted ind database.
     */
    fun insertItem(item: T): T? {
        val values = itemToContentValues(item)
        return if (values != null) {
            val insertId = db?.insert(tableName, null, values)?.toInt()
            Log.d(LOG_TAG, "Insert into $tableName id = $insertId")
            insertId?.let {
                getItem(insertId)
            } ?: null
//            getItem(insertId)
        } else {
            Log.e(LOG_TAG, "Unable to write empty item!")
            null
        }
    }

    /**
     * Convert item into [ContentValues]
     *
     * @param item Item to convert
     * @return Converted item into [ContentValues].
     */
    abstract fun itemToContentValues(item: T): ContentValues?

    /**
     * Delete item from database for table T.
     *
     * @param id Item id of element that will be deleted from table T.
     */
    fun deleteItem(id: Int) {
        Log.d(LOG_TAG, "$tableName deleted ID = $id")
        db?.delete(tableName, SQLiteHelper.Companion.COLUMN_ID + " = " + id, null)
    }

    /**
     * Update item in database for table T.
     *
     * @param item Item that will be updated.
     */
    fun updateItem(item: T): Int {
        val values = itemToContentValues(item)
        return if (values != null && values.containsKey(SQLiteHelper.Companion.COLUMN_ID)) {
            val where: String = (SQLiteHelper.Companion.COLUMN_ID + " = "
                    + values[SQLiteHelper.Companion.COLUMN_ID])
            val numberOfRecord = db?.update(tableName, values, where, null)
            Log.d(LOG_TAG, "Updated records count = $numberOfRecord")
            numberOfRecord?.let {
                numberOfRecord
            } ?: 0
        } else {
            Log.e(LOG_TAG, "Unable to update empty item!")
            0
        }
    }

    /**
     * Get all records from database for table T.
     *
     * @return List that contains all records of table T.
     */
    val all: ArrayList<T>
        get() {
            val cursor = queryLocal("SELECT * FROM " + tableName + " ORDER BY " + SQLiteHelper.Companion.COLUMN_DATE_ADDED + " DESC")
            return convertCursor(cursor)
        }

    /**
     * Get records from database for table T.
     *
     * @return List that contains all records of table T.
     */
    fun getRecords(page: Int): ArrayList<T> {
        val cursor = queryLocal("SELECT * FROM " + tableName
                + " ORDER BY " + SQLiteHelper.Companion.COLUMN_DATE_ADDED + " DESC"
                + " LIMIT " + AppConstants.DEFAULT_PER_PAGE
                + " OFFSET " + (page - 1) * AppConstants.DEFAULT_PER_PAGE)
        return convertCursor(cursor)
    }

    /**
     * Get total records count database for table T.
     *
     * @return Existing records count of table T.
     */
    val count: Int
        get() {
            val cursor = queryLocal("SELECT COUNT(*) FROM $tableName")
            return if (cursor != null) {
                cursor.moveToFirst()
                cursor.getInt(0)
            } else {
                -1
            }
        }

    /**
     * Get records from database for table T.
     *
     * @return List that contains all records of table T.
     */
    fun getRecords(page: Int, order: String): ArrayList<T> {
        val cursor = queryLocal("SELECT * FROM " + tableName
                + " ORDER BY " + order
                + " LIMIT " + AppConstants.DEFAULT_PER_PAGE
                + " OFFSET " + (page - 1) * AppConstants.DEFAULT_PER_PAGE)
        return convertCursor(cursor)
    }

    /**
     * Delete all records from the table
     *
     * @throws SQLException on error
     */
    @Throws(SQLException::class)
    fun deleteAll() {
        db?.execSQL("DELETE FROM $tableName")
    }

    /**
     * Get items that match the conditions from table T.
     *
     * @param where Conditions to select some items.
     * @return List of some records from table T.
     */
    fun getItems(where: String): ArrayList<T> {
        val cursor = queryLocal("SELECT * FROM "
                + tableName + " WHERE " + where)
        return convertCursor(cursor)
    }

    /**
     * Get item from table T.
     *
     * @param id Item id to select.
     * @return Selected item from table.
     */
    fun getItem(id: Int): T? {
        val cursor = queryLocal("SELECT * FROM " + tableName
                + " WHERE " + SQLiteHelper.Companion.COLUMN_ID + " = " + id)
        val list: List<T> = convertCursor(cursor)
        return if (list.size > 0) {
            list[0]
        } else null
    }

    /**
     * Convert [Cursor] into item T
     *
     * @param cursor Cursor.
     * @return T item which corresponds some table in database.
     */
    fun convertCursor(cursor: Cursor): ArrayList<T> {
        val items = ArrayList<T>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast && !cursor.isBeforeFirst) {
            items.add(recordToItem(cursor))
            cursor.moveToNext()
        }
        cursor.close()
        return if (items.size > 0) {
            items
        } else items
    }

    /**
     * Convert one record of [Cursor] into item T
     *
     * @param cursor Cursor positioned to item need to convert.
     * @return T item which corresponds some table in database.
     */
    abstract fun recordToItem(cursor: Cursor): T

    /**
     * Query to local SQLite database with write to log query text and query result.
     *
     * @param query Query string.
     * @return Cursor that contains query result.
     */
    fun queryLocal(query: String): Cursor {
        Log.d(LOG_TAG, "queryLocal: $query")
        val cursor = db?.rawQuery(query, null)
        cursor?.let {c->
            if (BuildConfig.DEBUG) {
                val data = StringBuilder("Cursor[")
                if (c.moveToFirst()) {
                    do {
                        val columnCount = c.columnCount
                        data.append("row[")
                        for (i in 0 until columnCount) {
                            data.append(c.getColumnName(i)).append(" = ")
                            when (c.getType(i)) {
                                Cursor.FIELD_TYPE_BLOB -> data.append("byte array")
                                Cursor.FIELD_TYPE_FLOAT -> data.append(c.getFloat(i))
                                Cursor.FIELD_TYPE_INTEGER -> data.append(c.getInt(i))
                                Cursor.FIELD_TYPE_NULL -> data.append("null")
                                Cursor.FIELD_TYPE_STRING -> data.append(c.getString(i))
                            }
                            if (i != columnCount - 1) {
                                data.append(", ")
                            }
                        }
                        data.append("]\n")
                    } while (c.moveToNext())
                }
                data.append("]")
                Log.d(LOG_TAG, data.toString())
            }
        }
        return cursor!!
    }

    //TODO: move this method
    val recordsDurations: List<Long>
        get() {
            val cursor = queryLocal("SELECT " + SQLiteHelper.Companion.COLUMN_DURATION + " FROM " + tableName)
            val items = ArrayList<Long>()
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                items.add(cursor.getLong(cursor.getColumnIndex(SQLiteHelper.Companion.COLUMN_DURATION)))
                cursor.moveToNext()
            }
            cursor.close()
            return if (items.size > 0) {
                items
            } else items
        }

    /**
     * Constructor.
     *
     * @param context   Application context.
     * @param tableName Table name.
     */
    init {
        dbHelper = SQLiteHelper(context)
        this.tableName = tableName
    }
}