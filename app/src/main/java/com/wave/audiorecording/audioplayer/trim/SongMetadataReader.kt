package com.wave.audiorecording.audioplayer.trim

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import java.util.HashMap

class SongMetadataReader(activity: Activity?, filename: String) {
    var GENRES_URI = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
    var mActivity: Activity? = null
    var mFilename = ""
    @JvmField
    var mTitle: String? = ""
    @JvmField
    var mArtist: String? = ""
    var mAlbum: String? = ""
    var mGenre = ""
    var mYear = -1
    private fun ReadMetadata() {
        // Get a map from genre ids to names
        val genreIdMap = HashMap<String, String>()
        var c = mActivity?.contentResolver?.query(
                GENRES_URI, arrayOf(
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME),
                null, null, null)
        c?.moveToFirst()
        while (c?.isAfterLast?.not() == true) {
            genreIdMap[c.getString(0)] = c.getString(1)
            c.moveToNext()
        }
        c?.close()
        mGenre = ""
        for (genreId in genreIdMap.keys) {
            c = mActivity?.contentResolver?.query(
                    makeGenreUri(genreId), arrayOf(MediaStore.Audio.Media.DATA),
                    MediaStore.Audio.Media.DATA + " LIKE \"" + mFilename + "\"",
                    null, null)
            if (c?.count != 0) {
                mGenre = genreIdMap[genreId].toString()
                break
            }
            c.close()
            c = null
        }
        val uri = MediaStore.Audio.Media.getContentUriForPath(mFilename)
        c = uri?.let {
            mActivity?.contentResolver?.query(
                    it, arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATA),
                MediaStore.Audio.Media.DATA + " LIKE \"" + mFilename + "\"",
                null, null)
        }
        if (c?.count == 0) {
            mTitle = getBasename(mFilename)
            mArtist = ""
            mAlbum = ""
            mYear = -1
            return
        }
        c?.moveToFirst()
        mTitle = getStringFromColumn(c, MediaStore.Audio.Media.TITLE)
        if (mTitle == null || mTitle?.length == 0) {
            mTitle = getBasename(mFilename)
        }
        mArtist = getStringFromColumn(c, MediaStore.Audio.Media.ARTIST)
        mAlbum = getStringFromColumn(c, MediaStore.Audio.Media.ALBUM)
        mYear = getIntegerFromColumn(c, MediaStore.Audio.Media.YEAR)
        c?.close()
    }

    private fun makeGenreUri(genreId: String): Uri {
        val CONTENTDIR = MediaStore.Audio.Genres.Members.CONTENT_DIRECTORY
        return Uri.parse(
                StringBuilder()
                        .append(GENRES_URI.toString())
                        .append("/")
                        .append(genreId)
                        .append("/")
                        .append(CONTENTDIR)
                        .toString())
    }

    private fun getStringFromColumn(c: Cursor?, columnName: String): String? {
        return c?.let {
            val index = it.getColumnIndexOrThrow(columnName)
            val value = it.getString(index)
            if (value != null && value.length > 0) {
                value
            } else null
        }
//        return if (value != null && value.length > 0) {
//            value
//        } else {
//            null
//        }
    }

    private fun getIntegerFromColumn(c: Cursor?, columnName: String): Int {
        var value = -1
        c?.let {
            val index = it.getColumnIndexOrThrow(columnName)
            value = it.getInt(index)
        }
        return value
    }

    private fun getBasename(filename: String): String {
        return filename.substring(filename.lastIndexOf('/') + 1,
                filename.lastIndexOf('.'))
    }

    init {
        mActivity = activity
        mFilename = filename
        mTitle = getBasename(filename)
        try {
            ReadMetadata()
        } catch (e: Exception) {
        }
    }
}