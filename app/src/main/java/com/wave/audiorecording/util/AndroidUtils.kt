package com.wave.audiorecording.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Point
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.R
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Android related utilities methods.
 */
object AndroidUtils {
    //audio format in which file after trim will be saved.
    const val AUDIO_FORMAT = ".wav"

    //audio mime type in which file after trim will be saved.
    const val AUDIO_MIME_TYPE = "audio/x-wav"
    val currentTime: Long
        get() = System.nanoTime() / 1000000

    /**
     * Convert density independent pixels value (dip) into pixels value (px).
     *
     * @param dp Value needed to convert
     * @return Converted value in pixels.
     */
    fun dpToPx(dp: Int): Float {
        return dpToPx(dp.toFloat())
    }

    /**
     * Convert density independent pixels value (dip) into pixels value (px).
     *
     * @param dp Value needed to convert
     * @return Converted value in pixels.
     */
    fun dpToPx(dp: Float): Float {
        return dp * Resources.getSystem().displayMetrics.density
    }

    /**
     * Convert pixels value (px) into density independent pixels (dip).
     *
     * @param px Value needed to convert
     * @return Converted value in pixels.
     */
    fun pxToDp(px: Int): Float {
        return pxToDp(px.toFloat())
    }

    /**
     * Convert pixels value (px) into density independent pixels (dip).
     *
     * @param px Value needed to convert
     * @return Converted value in pixels.
     */
    fun pxToDp(px: Float): Float {
        return px / Resources.getSystem().displayMetrics.density
    }

    fun convertMillsToPx(mills: Long, pxPerSec: Float): Int {
        // 1000 is 1 second evaluated in milliseconds
        return (mills * pxPerSec / 1000).toInt()
    }

    fun convertPxToMills(px: Long, pxPerSecond: Float): Int {
        return (1000 * px / pxPerSecond).toInt()
    }

    @JvmOverloads
    fun runOnUIThread(runnable: Runnable?, delay: Long = 0) {
        if (delay == 0L) {
            runnable?.let { AudioRecordingWavesApplication.applicationHandler?.post(it) }
        } else {
            runnable?.let {
                AudioRecordingWavesApplication.applicationHandler?.postDelayed(
                    it,
                    delay
                )
            }
        }
    }

    /**
     * Convert int array to byte array
     */
    fun int2byte(src: IntArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(src.size * 4)
        val intBuffer = byteBuffer.asIntBuffer()
        intBuffer.put(src)
        return byteBuffer.array()
    }

    @JvmStatic
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x
    }

    /**
     * Read sound file duration.
     *
     * @param file Sound file
     * @return Duration in microseconds.
     */
    fun readRecordDuration(file: File): Long {
        try {
            val extractor = MediaExtractor()
            var format: MediaFormat? = null
            var i: Int
            extractor.setDataSource(file.path)
            val numTracks = extractor.trackCount
            // find and select the first audio track present in the file.
            i = 0
            while (i < numTracks) {
                format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    extractor.selectTrack(i)
                    break
                }
                i++
            }
            if (i == numTracks) {
                throw IOException("No audio track found in $file")
            }
            if (format != null) {
                return format.getLong(MediaFormat.KEY_DURATION)
            }
        } catch (e: IOException) {
            Log.e("TAG", e.message.toString())
        }
        return -1
    }

    @JvmOverloads
    fun showSimpleDialog(
        activity: Activity?,
        icon: Int,
        resTitle: Int,
        resContent: String?,
        positiveListener: DialogInterface.OnClickListener?,
        negativeListener: DialogInterface.OnClickListener? = null
    ) {
        val builder = AlertDialog.Builder(ContextThemeWrapper(activity, R.style.AlertDialogCustom))
        builder.setTitle(resTitle).setIcon(icon).setMessage(resContent).setCancelable(false)
            .setPositiveButton(R.string.btn_yes) { dialog, id ->
                positiveListener?.onClick(dialog, id)
                dialog.dismiss()
            }.setNegativeButton(
                R.string.btn_no
            ) { dialog, id ->
                negativeListener?.onClick(dialog, id)
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

}