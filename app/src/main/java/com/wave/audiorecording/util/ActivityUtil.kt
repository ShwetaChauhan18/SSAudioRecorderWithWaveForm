package com.wave.audiorecording.util

import android.app.Activity
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.WindowManager
import com.wave.audiorecording.R

object ActivityUtil {
    /**
     * This fun is used to make status bar icon color black and given color in status bar color
     *
     * @param activity
     * @param colorId
     */
    fun makeStatusBarColor(activity: Activity, colorId: Int, manipulateColor: Boolean) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            if (manipulateColor) {
                window.statusBarColor = manipulateColor(colorId, 0.9f)
            } else {
                window.statusBarColor = activity.resources.getColor(colorId)
            }
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    /**
     * This fun is used to make status bar color
     *
     * @param activty
     */
    fun makeStatusBarBlack(activty: Activity) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && VERSION.SDK_INT < VERSION_CODES.M && activty.window != null) {
            activty.window.statusBarColor = activty.resources.getColor(R.color.black)
        }
    }

    /**
     * This fun is used to make status bar color
     *
     * @param activty
     */
    fun makeStatusBarBlackForThisTheme(activty: Activity, color: Int) {
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && VERSION.SDK_INT < VERSION_CODES.M && activty.window != null && !isColorDark(
                color
            )
        ) {
            activty.window.statusBarColor = activty.resources.getColor(R.color.black)
        }
    }

    /**
     * This fun is used to manipulate given color by given factor
     *
     * @param color
     * @param factor
     * @return
     */
    fun manipulateColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Math.round(Color.red(color) * factor)
        val g = Math.round(Color.green(color) * factor)
        val b = Math.round(Color.blue(color) * factor)
        return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255))
    }

    /**
     * This fun is used to check whether color is dark or light
     *
     * @param color
     * @return
     */
    fun isColorDark(color: Int): Boolean {
        val darkness: Double =
            1 - (Constants.Intents.Companion.NUMBER_0_POINT_299 * Color.red(color) + Constants.Intents.Companion.NUMBER_0_POINT_587 * Color.green(
                color
            ) + Constants.Intents.Companion.NUMBER_0_POINT_114 * Color.blue(color)) / Constants.Intents.Companion.NUMBER_255
        return darkness >= 0.5
    }

}