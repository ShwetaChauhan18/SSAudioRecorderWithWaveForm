package com.wave.audiorecording.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.StatFs
import com.wave.audiorecording.R
import java.util.Locale

object DeviceUtils {
    @JvmStatic
    fun isTabletDevice(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.tablet)
    }
}