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

    fun <T : View?> find(view: View, id: Int): T {
        return view.findViewById<View>(id) as T
    }

    fun <T : View?> find(activity: Activity, id: Int): T {
        return activity.findViewById<View>(id) as T
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

}