package com.wave.audiorecording.util

import android.content.Context
import com.wave.audiorecording.R

object DeviceUtils {
    @JvmStatic
    fun isTabletDevice(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.tablet)
    }
}