package com.wave.audiorecording.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.StatFs
import com.wave.audiorecording.R
import java.util.Locale

object DeviceUtils {
    val isNexus5x: Boolean
        get() = Build.MODEL.toLowerCase(Locale.US).replace(" ", "") == "nexus5x"

    @JvmStatic
    fun isTabletDevice(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.tablet)
    }

    fun isPortrait(context: Context): Boolean {
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_PORTRAIT
    }

    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    fun getScreenDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    fun getDeviceFreeSpace(context: Context): Float {
        val stat = StatFs(context.externalCacheDir!!.path)
        val bytesAvailable = stat.blockSize.toLong() * stat.blockCount.toLong()
        val bytesFree = stat.blockSize.toLong() * stat.freeBlocks.toLong()
        val megAvailable = bytesAvailable / 1048576
        val megFree = bytesFree / 1048576
        return megFree.toFloat() / megAvailable.toFloat()
    }
}