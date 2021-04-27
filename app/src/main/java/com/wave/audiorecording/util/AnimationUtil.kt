package com.wave.audiorecording.util

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.TargetApi
import android.content.Context
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils

object AnimationUtil {
    @TargetApi(21)
    fun viewElevationAnimation(view: View?, `val`: Float, listener: Animator.AnimatorListener?) {
        view?.animate()
                ?.translationZ(`val`)
                ?.setDuration(250L)
                ?.setInterpolator(AnimationUtils.loadInterpolator(view.context,
                        android.R.interpolator.accelerate_decelerate))
                ?.setListener(listener)
                ?.start()
    }

    @TargetApi(21)
    fun viewAnimationX(view: View?, `val`: Float) {
        view?.animate()
                ?.translationX(`val`)
                ?.alpha(0.0f)
                ?.setDuration(300)
                ?.start()
    }

    @TargetApi(21)
    fun viewAnimationX(view: View?, `val`: Float, listener: Animator.AnimatorListener?) {
        view?.animate()
                ?.translationX(`val`)
                ?.alpha(0.9f)
                ?.setDuration(300L)
                ?.setInterpolator(AnimationUtils.loadInterpolator(view.context,
                        android.R.interpolator.accelerate_decelerate))
                ?.setListener(listener)
                ?.start()
    }

    @TargetApi(21)
    fun viewAnimationY(view: View?, `val`: Float, listener: Animator.AnimatorListener?) {
        view?.animate()
                ?.translationY(`val`)
                ?.setDuration(250L)
                ?.setInterpolator(AnimationUtils.loadInterpolator(view.context,
                        android.R.interpolator.accelerate_decelerate))
                ?.setListener(listener)
                ?.start()
    }

    fun animation(view: View?) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("scaleX", 1.04f),
                PropertyValuesHolder.ofFloat("scaleY", 1.04f))
        scaleDown.duration = 2000
        scaleDown.repeatCount = ObjectAnimator.INFINITE
        scaleDown.repeatMode = ObjectAnimator.REVERSE
        scaleDown.start()
    }

    fun animation(mContext: Context?, view: View?, anim: Int, listener: Animation.AnimationListener?) {
        val animation = AnimationUtils.loadAnimation(mContext, anim)
        animation.setAnimationListener(listener)
        view?.startAnimation(animation)
    }

    fun pauseButtonAnimation(view: View?, isAnimationStart: Boolean) {
        val anim: Animation = AlphaAnimation(0.4f, 1.0f)
        anim.duration = 1300 //You can manage the blinking time with this parameter
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        if (isAnimationStart) view?.startAnimation(anim) else view?.clearAnimation()
    }
}