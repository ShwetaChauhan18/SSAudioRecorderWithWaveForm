package com.wave.audiorecording.util

import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.wave.audiorecording.exception.CantCreateFileException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileUtil {
    /**
     * Represents the end-of-file (or stream).
     */
    const val EOF = -1
    private const val LOG_TAG = "FileUtil"

    /**
     * The default buffer size ({@value}) to use for
     * [.copyLarge]
     */
    private const val DEFAULT_BUFFER_SIZE = 1024 * 4
    fun getAppDir(context: Context): File? {
        return getStorageDir(AppConstants.APPLICATION_NAME, context)
    }

    @Throws(FileNotFoundException::class)
    fun getPrivateRecordsDir(context: Context): File {
        return getPrivateMusicStorageDir(context, AppConstants.RECORDS_DIR)
                ?: throw FileNotFoundException()
    }

    fun generateRecordNameCounted(counter: Long): String {
        return AppConstants.BASE_RECORD_NAME + counter
    }

    fun generateRecordNameDate(): String {
        return AppConstants.BASE_RECORD_NAME_SHORT + TimeUtils.formatDateForName(System.currentTimeMillis())
    }

    fun addExtension(name: String, extension: String): String {
        return name + AppConstants.EXTENSION_SEPARATOR + extension
    }

    /**
     * Remove file extension from file name;
     *
     * @param name File name with extension;
     * @return File name without extension or unchanged String if extension was not identified.
     */
    fun removeFileExtension(name: String): String {
        return if (name.contains(AppConstants.EXTENSION_SEPARATOR)) {
            name.substring(0, name.lastIndexOf(AppConstants.EXTENSION_SEPARATOR))
        } else name
    }

    /**
     * @param input  the `InputStream` to read from
     * @param output the `OutputStream` to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun copyLarge(input: InputStream, output: OutputStream, buffer: ByteArray? = ByteArray(DEFAULT_BUFFER_SIZE), listener: OnCopyListener? = null): Long {
        var count: Long = 0
        var n: Int
        val size = input.available().toLong()
        var percent = 0
        var stepPercent: Long = 0
        var isCancel = false
        while (EOF != input.read(buffer).also { n = it } && !isCancel) {
            output.write(buffer, 0, n)
            count += n.toLong()
            percent = (100 * count.toFloat() / size.toFloat()).toInt()
            if (listener != null) {
                isCancel = listener.isCancel
                if (percent > stepPercent + 1 || count == size) {
                    listener.onCopyProgress(percent, count, size)
                    stepPercent = percent.toLong()
                }
            }
        }
        if (listener != null) {
            if (isCancel) {
                listener.onCanceled()
                return -1
            } else {
                listener.onCopyFinish()
            }
        }
        return count
    }

    /**
     * Copy file.
     *
     * @param fileToCopy File to copy.
     * @param newFile    File in which will contain copied data.
     * @return true if copy succeed, otherwise - false.
     */
    @Throws(IOException::class)
    fun copyFile(fileToCopy: FileDescriptor?, newFile: File?): Boolean {
        var `in`: FileInputStream? = null
        var out: FileOutputStream? = null
        return try {
            `in` = FileInputStream(fileToCopy)
            out = FileOutputStream(newFile)
            if (copyLarge(`in`, out) > 0) {
                true
            } else {
                Log.e("TAG", "Nothing was copied!")
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            `in`?.close()
            out?.close()
        }
    }

    /**
     * Copy file.
     *
     * @param fileToCopy File to copy.
     * @param newFile    File in which will contain copied data.
     * @return true if copy succeed, otherwise - false.
     */
    @Throws(IOException::class)
    fun copyFile(fileToCopy: File, newFile: File): Boolean {
        Log.v("TAG", "copyFile toCOpy = " + fileToCopy.absolutePath + " newFile = " + newFile.absolutePath)
        var `in`: FileInputStream? = null
        var out: FileOutputStream? = null
        return try {
            `in` = FileInputStream(fileToCopy)
            out = FileOutputStream(newFile)
            if (copyLarge(`in`, out) > 0) {
                true
            } else {
                Log.e("TAG", "Nothing was copied!")
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            `in`?.close()
            out?.close()
        }
    }

    /**
     * Copy file.
     *
     * @param fileToCopy File to copy.
     * @param newFile    File in which will contain copied data.
     * @return true if copy succeed, otherwise - false.
     */
    fun copyFile(fileToCopy: File?, newFile: File, listener: OnCopyListener?): Boolean {
        try {
            FileInputStream(fileToCopy).use { `in` ->
                FileOutputStream(newFile).use { out ->
                    return if (copyLarge(`in`, out, ByteArray(DEFAULT_BUFFER_SIZE), listener) > 0) {
                        true
                    } else {
                        Log.e("TAG", "Nothing was copied!")
                        deleteFile(newFile)
                        false
                    }
                }
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Get free space for specified file
     *
     * @param f Dir
     * @return Available space for specified file in bytes
     */
    fun getFree(f: File?): Long {
        var f = f
        while (f?.exists()?.not() == true) {
            f = f.parentFile
            if (f == null) return 0
        }
        val fsi = StatFs(f?.path)
        return if (VERSION.SDK_INT >= 18) {
            fsi.blockSizeLong * fsi.availableBlocksLong
        } else {
            fsi.blockSize * fsi.availableBlocks.toLong()
        }
    }

    fun getAvailableInternalMemorySize(context: Context): Long {
        val file = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return if (file != null) {
            val fsi = StatFs(file.absolutePath)
            if (VERSION.SDK_INT >= 18) {
                fsi.blockSizeLong * fsi.availableBlocksLong
            } else {
                fsi.blockSize * fsi.availableBlocks.toLong()
            }
        } else {
            0
        }
    }

    fun getAvailableExternalMemorySize(context: Context): Long {
        return if (externalMemoryAvailable()) {
            val path = context.externalCacheDir
            val fsi = StatFs(path?.path)
            if (VERSION.SDK_INT >= 18) {
                fsi.blockSizeLong * fsi.availableBlocksLong
            } else {
                fsi.blockSize * fsi.availableBlocks.toLong()
            }
        } else {
            0
        }
    }

    fun externalMemoryAvailable(): Boolean {
        return Environment.getExternalStorageState() ==
                Environment.MEDIA_MOUNTED
    }

    /**
     * Create file.
     * If it is not exists, than create it.
     *
     * @param path     Path to file.
     * @param fileName File name.
     */
    fun createFile(path: File?, fileName: String?): File? {
        return if (path != null) {
            createDir(path)
            Log.d(LOG_TAG, "createFile path = " + path.absolutePath + " fileName = " + fileName)
            val file = File(path, fileName)
            //Create file if need.
            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        Log.i(LOG_TAG, "The file was successfully created! - " + file.absolutePath)
                    } else {
                        Log.i(LOG_TAG, "The file exist! - " + file.absolutePath)
                    }
                } catch (e: IOException) {
                    Log.e(LOG_TAG, "Failed to create the file.", e)
                    return null
                }
            } else {
                Log.e(LOG_TAG, "File already exists? Please rename file!")
                Log.i(LOG_TAG, "Renaming file")
                //				TODO: Find better way to rename file.
                return createFile(path, "1$fileName")
            }
            if (!file.canWrite()) {
                Log.e(LOG_TAG, "The file can not be written.")
            }
            file
        } else {
            null
        }
    }

    fun createDir(dir: File?): File? {
        if (dir != null) {
            if (!dir.exists()) {
                try {
                    if (dir.mkdirs()) {
                        Log.d(LOG_TAG, "Dirs are successfully created")
                        return dir
                    } else {
                        Log.e(LOG_TAG, "Dirs are NOT created! Please check permission write to external storage!")
                    }
                } catch (e: Exception) {
                    Log.e("TAG", e.message.toString())
                }
            } else {
                Log.d(LOG_TAG, "Dir already exists")
                return dir
            }
        }
        Log.e(LOG_TAG, "File is null or unable to create dirs")
        return null
    }

    /**
     * Get public external storage directory
     *
     * @param dirName Directory name.
     */
    fun getStorageDir(dirName: String?, context: Context): File? {
        return if (dirName != null && !dirName.isEmpty()) {
            val file = File(context.externalCacheDir, dirName)
            if (isExternalStorageReadable && isExternalStorageWritable) {
//				if (!file.exists() && !file.mkdirs()) {
//					Log.e(LOG_TAG, "Directory " + file.getAbsolutePath() + " was not created");
//				}
                createDir(file)
            } else {
                Log.e(LOG_TAG, "External storage are not readable or writable")
            }
            file
        } else {
            null
        }
    }

    /**
     * Checks if external storage is available for read and write.
     */
    val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    /**
     * Checks if external storage is available to at least read.
     */
    val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    fun isFileInExternalStorage(context: Context, path: String?): Boolean {
        var privateDir: String? = ""
        try {
            privateDir = getPrivateRecordsDir(context).absolutePath
        } catch (e: FileNotFoundException) {
            Log.e("TAG", e.message.toString())
        }
        return path == null || !path.contains(privateDir.toString())
    }

    fun getPublicMusicStorageDir(albumName: String?): File {
        val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), albumName)
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created")
        }
        return file
    }

    fun getPrivateMusicStorageDir(context: Context, albumName: String?): File? {
        val file = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (file != null) {
            val f = File(file, albumName)
            if (!f.exists() && !f.mkdirs()) {
                Log.e(LOG_TAG, "Directory not created")
            } else {
                return f
            }
        }
        return null
    }

    fun renameFile(file: File, newName: String, extension: String): Boolean {
        if (!file.exists()) {
            return false
        }
        Log.v("TAG", "old File: " + file.absolutePath)
        val renamed = File(file.parentFile.absolutePath + File.separator + newName + AppConstants.EXTENSION_SEPARATOR + extension)
        Log.v("TAG", "new File: " + renamed.absolutePath)
        if (!file.renameTo(renamed)) {
            if (!file.renameTo(renamed)) {
                return file.renameTo(renamed)
            }
        }
        return true
    }

    fun renameFile(file: File, renamed: File): Boolean {
        if (!file.exists()) {
            return false
        }
        Log.v("TAG", "old File: " + file.absolutePath)
        Log.v("TAG", "new File: " + renamed.absolutePath)
        if (!file.renameTo(renamed)) {
            if (!file.renameTo(renamed)) {
                return file.renameTo(renamed)
            }
        }
        return true
    }

    fun removeUnallowedSignsFromName(name: String): String {
//		String str = name.replaceAll("[^a-zA-Z0-9\\.\\-\\_]", "_");
//		return str.trim();
        return name.trim { it <= ' ' }
    }

    /**
     * Remove file or directory with all content
     *
     * @param file File or directory needed to delete.
     */
    fun deleteFile(file: File): Boolean {
        if (deleteRecursivelyDirs(file)) {
            return true
        }
        Log.e(LOG_TAG, "Failed to delete directory: " + file.absolutePath)
        return false
    }

    /**
     * Recursively remove file or directory with children.
     *
     * @param file File to remove
     */
    private fun deleteRecursivelyDirs(file: File?): Boolean {
        var ok = true
        if (file != null && file.exists()) {
            if (file.isDirectory) {
                val children = file.list()
                for (i in children.indices) {
                    ok = ok and deleteRecursivelyDirs(File(file, children[i]))
                }
            }
            if (ok && file.delete()) {
                Log.d(LOG_TAG, "File deleted: " + file.absolutePath)
            }
        }
        return ok
    }

    private fun isVirtualFile(context: Context, uri: Uri): Boolean {
        return if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            if (!DocumentsContract.isDocumentUri(context, uri)) {
                return false
            }
            val cursor = context.contentResolver.query(
                    uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                    null, null, null)
            var flags = 0
            if (cursor?.moveToFirst() == true) {
                flags = cursor.getInt(0)
            }
            cursor?.close()
            flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
        } else {
            false
        }
    }

    private fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    private fun replaceFileWithDir(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) {
            return file.mkdirs()
        } else if (file.delete()) {
            val folder = File(path)
            return folder.mkdirs()
        }
        return false
    }

    fun copyFile(context: Context, uri: Uri): File? {
        try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor = parcelFileDescriptor?.fileDescriptor
            val name = extractFileName(context, uri)
            val newFile = provideRecordFile(name, context)
            if (copyFile(fileDescriptor, newFile)) {
                return newFile
            }
        } catch (e: SecurityException) {
            Log.e("FileUtil", e.message.toString())
        } catch (e: IOException) {
            Log.e("FileUtil", e.message.toString())
        } catch (e: OutOfMemoryError) {
            Log.e("FileUtil", e.message.toString())
        } catch (e: IllegalStateException) {
            Log.e("FileUtil", e.message.toString())
        } catch (ex: CantCreateFileException) {
            Log.e("FileUtil", ex.message.toString())
        }
        return null
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

    @Throws(CantCreateFileException::class)
    private fun provideRecordFile(name: String?, context: Context): File {
        val recordFile = createFile(getAppDir(context), name)
        if (recordFile != null) {
            return recordFile
        }
        throw CantCreateFileException()
    }

    fun haxtoFloat(hsxString: String): Float {
        val i = hsxString.toLong(16)
        return java.lang.Float.intBitsToFloat(i.toInt())
    }
}