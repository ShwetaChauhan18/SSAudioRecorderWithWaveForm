package com.wave.audiorecording.util

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ScaleXSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.wave.audiorecording.R
import java.io.IOException
import java.util.HashMap

object ViewUtils {
    private val TYPEFACE_CACHE: MutableMap<String, Typeface> = HashMap()
    fun animateBackgroundColor(view: ViewGroup, start: Int, end: Int) {
        val colorFrom = view.resources.getColor(start)
        val colorTo = view.resources.getColor(end)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 300L
        colorAnimation.addUpdateListener { animator: ValueAnimator -> view.setBackgroundColor((animator.animatedValue as Int)) }
        colorAnimation.start()
    }

    fun animateBackgroundColor(view: ViewGroup, colorFrom: Int?, colorTo: Int?) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 300L
        colorAnimation.addUpdateListener { animator: ValueAnimator -> view.setBackgroundColor((animator.animatedValue as Int)) }
        colorAnimation.start()
    }

    fun applyTextKerning(src: CharSequence?, kerning: Float): Spannable? {
        if (src == null) {
            return null
        }
        val srcLength = src.length
        if (srcLength < 2) {
            return if (src is Spannable) src else SpannableString(src)
        }
        val nonBreakingSpace = " "
        val builder = if (src is SpannableStringBuilder) src else SpannableStringBuilder(src)
        for (i in src.length downTo 1) {
            builder.insert(i, nonBreakingSpace)
            builder.setSpan(ScaleXSpan(kerning), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
    }

    @JvmOverloads
    fun calculateGridColumns(activity: Activity, maxWidth: Int = 200, def: Int = 2): Int {
        if (DeviceUtils.isTabletDevice(activity)) {
            val display = activity.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = activity.resources.displayMetrics.density
            val dpHeight = outMetrics.widthPixels / density
            return dpHeight.toInt() / maxWidth
        }
        return def
    }

    fun convertThemeColour(colour: String?): Int? {
        return if (colour == null || colour.trim { it <= ' ' }.isEmpty() || !colour.startsWith("#")) {
            null
        } else Color.parseColor(colour)
    }


    fun darkerColour(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(a,
                Math.max((r * factor).toInt(), 0),
                Math.max((g * factor).toInt(), 0),
                Math.max((b * factor).toInt(), 0))
    }

    fun <T : View?> find(view: View, id: Int): T {
        return view.findViewById<View>(id) as T
    }

    fun <T : View?> find(activity: Activity, id: Int): T {
        return activity.findViewById<View>(id) as T
    }

    fun getDisplayWidth(context: Context): Int {
        return getDisplayMetrics(context).widthPixels
    }

    fun getDisplayHeight(context: Context): Int {
        return getDisplayMetrics(context).heightPixels
    }

    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics
    }

    fun getTypeface(context: Context, name: String): Typeface? {
        if (TYPEFACE_CACHE.containsKey(name)) {
            return TYPEFACE_CACHE[name]
        }
        val file = "fonts/$name.ttf"
        val typeface = Typeface.createFromAsset(context.applicationContext.assets, file)
        TYPEFACE_CACHE[name] = typeface
        return typeface
    }

    fun hideKeyboard(activity: Activity?) {
        if (activity != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
    }

    fun showKeyboard(activity: Activity?) {
        if (activity != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
        }
    }

    fun <T : View?> inflate(parent: ViewGroup, layout: Int): T {
        return LayoutInflater.from(parent.context).inflate(layout, parent, false) as T
    }

    fun <T : View?> inflate(context: Context?, layout: Int): T {
        return LayoutInflater.from(context).inflate(layout, null, false) as T
    }

    fun setViewBackgroundDrawable(view: View, drawable: Drawable?) {
        if (VERSION.SDK_INT > VERSION_CODES.JELLY_BEAN) {
            view.background = drawable
        } else {
            view.setBackgroundDrawable(drawable)
        }
    }

    fun enableRotation(activity: Activity?, enabled: Boolean) {
        if (activity == null) {
            return
        }
        try {
            if (enabled) {
                val currentOrientation = activity.resources.configuration.orientation
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                } else {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setViewWidth(view: View, width: Int) {
        val params = view.layoutParams
        params.width = width
        view.layoutParams = params
    }

    fun setViewHeight(view: View, height: Int) {
        val params = view.layoutParams
        params.height = height
        view.layoutParams = params
    }
}