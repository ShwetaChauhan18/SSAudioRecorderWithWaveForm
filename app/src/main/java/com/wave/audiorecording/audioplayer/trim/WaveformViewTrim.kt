package com.wave.audiorecording.audioplayer.trim

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import com.wave.audiorecording.R

/**
 * WaveformView is an Android view that displays a visual representation
 * of an audio waveform.  It retrieves the frame gains from a CheapSoundFile
 * object and recomputes the shape contour at several zoom levels.
 *
 *
 * This class doesn't handle selection or any of the touch interactions
 * directly, so it exposes a listener interface.  The class that embeds
 * this view should add itself as a listener and make the view scroll
 * and respond to other events appropriately.
 *
 *
 * WaveformView doesn't actually handle selection, but it will just display
 * the selected part of the waveform in a different color.
 */
class WaveformViewTrim(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var isDrawBorder = true

    // Colors
    private val mGridPaint: Paint
    private val mSelectedLinePaint: Paint
    private val mUnselectedLinePaint: Paint
    private val mUnselectedBkgndLinePaint: Paint
    private val mBorderLinePaint: Paint
    private val mPlaybackLinePaint: Paint
    private val mTimecodePaint: Paint
    private var mSoundFile: SoundFile?
    private var mLenByZoomLevel: IntArray?
    private var mValuesByZoomLevel: Array<DoubleArray?>?
    private var mZoomFactorByZoomLevel: DoubleArray = DoubleArray(5)
    private var mHeightsAtThisZoomLevel: IntArray?
    private var mZoomLevel = 0
    private var mNumZoomLevels = 0
    private var mSampleRate = 0
    private var mSamplesPerFrame = 0
    private var mOffset: Int
    private var mSelectionStart: Int
    private var mSelectionEnd: Int
    private var mPlaybackPos: Int
    private var mDensity: Float
    private var mInitialScaleSpan = 0f
    private var mListener: WaveformListener? = null
    private val mGestureDetector: GestureDetector
    private val mScaleGestureDetector: ScaleGestureDetector
    private var mInitialized: Boolean
    fun isDrawBorder(): Boolean {
        return isDrawBorder
    }

    fun setIsDrawBorder(isDrawBorder: Boolean) {
        this.isDrawBorder = isDrawBorder
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        if (mGestureDetector.onTouchEvent(event)) {
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> mListener?.waveformTouchStart(event.x)
            MotionEvent.ACTION_MOVE -> mListener?.waveformTouchMove(event.x)
            MotionEvent.ACTION_UP -> mListener?.waveformTouchEnd()
        }
        return true
    }

    fun setSoundFile(soundFile: SoundFile?) {
        mSoundFile = soundFile
        mSampleRate = mSoundFile?.getSampleRate() ?: 0
        mSamplesPerFrame = mSoundFile?.getSamplesPerFrame() ?: 0
        computeDoublesForAllZoomLevels()
        mHeightsAtThisZoomLevel = null
    }

    fun isInitialized(): Boolean {
        return mInitialized
    }

    fun canZoomIn(): Boolean {
        return mZoomLevel > 0
    }

    fun zoomIn() {
        if (canZoomIn()) {
            mZoomLevel--
            mSelectionStart *= 2
            mSelectionEnd *= 2
            mHeightsAtThisZoomLevel = null
            var offsetCenter = mOffset + measuredWidth / 2
            offsetCenter *= 2
            mOffset = offsetCenter - measuredWidth / 2
            if (mOffset < 0) mOffset = 0
            invalidate()
        }
    }

    fun canZoomOut(): Boolean {
        return mZoomLevel < mNumZoomLevels - 1
    }

    fun zoomOut() {
        if (canZoomOut()) {
            mZoomLevel++
            mSelectionStart /= 2
            mSelectionEnd /= 2
            var offsetCenter = mOffset + measuredWidth / 2
            offsetCenter /= 2
            mOffset = offsetCenter - measuredWidth / 2
            if (mOffset < 0) mOffset = 0
            mHeightsAtThisZoomLevel = null
            invalidate()
        }
    }

    fun maxPos(): Int? {
        Log.e("MAX", mLenByZoomLevel?.get(mZoomLevel).toString() + "")
        return mLenByZoomLevel?.get(mZoomLevel)
    }

    fun secondsToFrames(seconds: Double): Int {
        return (1.0 * seconds * mSampleRate / mSamplesPerFrame + 0.5).toInt()
    }

    fun secondsToPixels(seconds: Double): Int {
        val z = mZoomFactorByZoomLevel[mZoomLevel]
        return (z * seconds * mSampleRate / mSamplesPerFrame + 0.5).toInt()
    }

    fun pixelsToSeconds(pixels: Int): Double {
        val z = mZoomFactorByZoomLevel[mZoomLevel]
        return pixels * mSamplesPerFrame.toDouble() / (mSampleRate * z)
    }

    fun millisecsToPixels(msecs: Int): Int {
        val z = mZoomFactorByZoomLevel[mZoomLevel]
        return (msecs * 1.0 * mSampleRate * z /
                (1000.0 * mSamplesPerFrame) + 0.5).toInt()
    }

    fun pixelsToMillisecs(pixels: Int): Int {
        val z = mZoomFactorByZoomLevel[mZoomLevel]
        return (pixels * (1000.0 * mSamplesPerFrame) /
                (mSampleRate * z) + 0.5).toInt()
    }

    fun setParameters(start: Int, end: Int, offset: Int) {
        mSelectionStart = start
        mSelectionEnd = end
        mOffset = offset
    }

    fun getEnd(): Int {
        return mSelectionEnd
    }

    fun setPlayback(pos: Int) {
        mPlaybackPos = pos
    }

    fun setListener(listener: WaveformListener?) {
        mListener = listener
    }

    fun recomputeHeights(density: Float) {
        mHeightsAtThisZoomLevel = null
        mDensity = density
        mTimecodePaint.setTextSize((12 * density))
        invalidate()
    }

    protected fun drawWaveformLine(canvas: Canvas, x: Int, y0: Int, y1: Int, paint: Paint?) {
        paint?.let { canvas.drawLine(x.toFloat(), y0.toFloat(), x.toFloat(), y1.toFloat(), it) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mSoundFile == null) return
        if (mHeightsAtThisZoomLevel == null) computeIntsForThisZoomLevel()

        // Draw waveform
        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight
        val start = mOffset
        var width = (mHeightsAtThisZoomLevel?.size ?: 0) - start
        val ctr = measuredHeight / 2
        if (width > measuredWidth) width = measuredWidth

        // Draw grid
        val onePixelInSecs = pixelsToSeconds(1)
        val onlyEveryFiveSecs = onePixelInSecs > 1.0 / 50.0
        var fractionalSecs = mOffset * onePixelInSecs
        var integerSecs = fractionalSecs.toInt()
        var i = 0
        while (i < width) {
            i++
            fractionalSecs += onePixelInSecs
            val integerSecsNew = fractionalSecs.toInt()
            if (integerSecsNew != integerSecs) {
                integerSecs = integerSecsNew
                if (!onlyEveryFiveSecs || 0 == integerSecs % 5) {
                    canvas.drawLine(i.toFloat(), 0f, i.toFloat(), measuredHeight.toFloat(), mGridPaint)
                }
            }
        }

        // Draw waveform
        i = 0
        while (i < width) {
            var paint: Paint
            paint = if (i + start >= mSelectionStart &&
                    i + start < mSelectionEnd) {
                mSelectedLinePaint
            } else {
                drawWaveformLine(canvas, i, 0, measuredHeight, mUnselectedBkgndLinePaint)
                mUnselectedLinePaint
            }
            drawWaveformLine(
                    canvas, i,
                    ctr - (mHeightsAtThisZoomLevel?.get(start + i) ?: 0),
                    ctr + 1 + (mHeightsAtThisZoomLevel?.get(start + i) ?: 0),
                    paint)
            if (i + start == mPlaybackPos) {
                canvas.drawLine(i.toFloat(), 0f, i.toFloat(), measuredHeight.toFloat(), mPlaybackLinePaint)
            }
            i++
        }

        // If we can see the right edge of the waveform, draw the
        // non-waveform area to the right as unselected
        i = width
        while (i < measuredWidth) {
            drawWaveformLine(canvas, i, 0, measuredHeight, mUnselectedBkgndLinePaint)
            i++
        }

        // Draw borders
        if (isDrawBorder()) {
            canvas.drawLine(
                    mSelectionStart - mOffset + 0.5f, 0f,
                    mSelectionStart - mOffset + 0.5f, measuredHeight.toFloat(),
                    mBorderLinePaint)
            canvas.drawLine(
                    mSelectionEnd - mOffset + 0.5f, 0f,
                    mSelectionEnd - mOffset + 0.5f, measuredHeight.toFloat(),
                    mBorderLinePaint)
        }

        /*// Draw timecode
        double timecodeIntervalSecs = 1.0;
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 5.0;
        }
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 15.0;
        }

        // Draw grid
        fractionalSecs = mOffset * onePixelInSecs;
        int integerTimecode = (int) (fractionalSecs / timecodeIntervalSecs);
        i = 0;
        while (i < width) {
            i++;
            fractionalSecs += onePixelInSecs;
            integerSecs = (int) fractionalSecs;
            int integerTimecodeNew = (int) (fractionalSecs /
                                            timecodeIntervalSecs);
            if (integerTimecodeNew != integerTimecode) {
                integerTimecode = integerTimecodeNew;

                // Turn, e.g. 67 seconds into "1:07"
                String timecodeMinutes = "" + (integerSecs / 60);
                String timecodeSeconds = "" + (integerSecs % 60);
                if ((integerSecs % 60) < 10) {
                    timecodeSeconds = "0" + timecodeSeconds;
                }
                String timecodeStr = timecodeMinutes + ":" + timecodeSeconds;
                float offset = (float) (
                    0.5 * mTimecodePaint.measureText(timecodeStr));
                canvas.drawText(timecodeStr,
                                i - offset,
                                (int)(12 * mDensity),
                                mTimecodePaint);
            }
        }*/if (mListener != null) {
            mListener?.waveformDraw()
        }
    }

    /**
     * Called once when a new sound file is added
     */
    private fun computeDoublesForAllZoomLevels() {
        val numFrames = mSoundFile?.getNumFrames() ?: 0
        val frameGains = mSoundFile?.getFrameGains()
        val smoothedGains = DoubleArray(numFrames)
        frameGains?.let {
            if (numFrames == 1) {
                smoothedGains[0] = it.get(0).toDouble()
            } else if (numFrames == 2) {
                smoothedGains[0] = it.get(0).toDouble()
                smoothedGains[1] = it.get(1).toDouble()
            } else if (numFrames > 2) {
                smoothedGains[0] = (it.get(0) / 2.0 +
                        it.get(1) / 2.0)
                for (i in 1 until numFrames - 1) {
                    smoothedGains[i] = (it.get(i - 1) / 3.0 +
                            it.get(i) / 3.0 +
                            it.get(i + 1) / 3.0)
                }
                smoothedGains[numFrames - 1] = (it.get(numFrames - 2) / 2.0 +
                        it.get(numFrames - 1) / 2.0)
            }
        }

        // Make sure the range is no more than 0 - 255
        var maxGain = 1.0
        for (i in 0 until numFrames) {
            if (smoothedGains[i] > maxGain) {
                maxGain = smoothedGains[i]
            }
        }
        var scaleFactor = 1.0
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain
        }

        // Build histogram of 256 bins and figure out the new scaled max
        maxGain = 0.0
        val gainHist = IntArray(256)
        for (i in 0 until numFrames) {
            var smoothedGain = (smoothedGains[i] * scaleFactor).toInt()
            if (smoothedGain < 0) smoothedGain = 0
            if (smoothedGain > 255) smoothedGain = 255
            if (smoothedGain > maxGain) maxGain = smoothedGain.toDouble()
            gainHist[smoothedGain]++
        }

        // Re-calibrate the min to be 5%
        var minGain = 0.0
        var sum = 0
        while (minGain < 255 && sum < numFrames / 20) {
            sum += gainHist[minGain.toInt()]
            minGain++
        }

        // Re-calibrate the max to be 99%
        sum = 0
        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[maxGain.toInt()]
            maxGain--
        }

        // Compute the heights
        val heights = DoubleArray(numFrames)
        val range = maxGain - minGain
        for (i in 0 until numFrames) {
            var value = (smoothedGains[i] * scaleFactor - minGain) / range
            if (value < 0.0) value = 0.0
            if (value > 1.0) value = 1.0
            heights[i] = value * value
        }
        mNumZoomLevels = 5
        mLenByZoomLevel = IntArray(5)
        mZoomFactorByZoomLevel = DoubleArray(5)
        mValuesByZoomLevel = arrayOfNulls(5)

        // Level 0 is doubled, with interpolated values
        mLenByZoomLevel?.set(0, (numFrames * 2))
        mZoomFactorByZoomLevel[0] = 2.0
        mValuesByZoomLevel?.set(0, DoubleArray(mLenByZoomLevel?.get(0) ?: 0))
        if (numFrames > 0) {
            mValuesByZoomLevel?.get(0)?.set(0, 0.5 * heights[0])
            mValuesByZoomLevel?.get(0)?.set(1, heights[0])
        }
        for (i in 1 until numFrames) {
            mValuesByZoomLevel?.get(0)?.set(2 * i, 0.5 * (heights[i - 1] + heights[i]))
            mValuesByZoomLevel?.get(0)?.set(2 * i + 1, heights[i])
        }

        // Level 1 is normal
        mLenByZoomLevel?.set(1, numFrames)
        mValuesByZoomLevel?.set(1, DoubleArray(mLenByZoomLevel?.get(1) ?: 0))
        mZoomFactorByZoomLevel[1] = 1.0
        for (i in 0 until (mLenByZoomLevel?.get(1) ?: 0)) {
            mValuesByZoomLevel?.get(1)?.set(i, heights[i])
        }

        // 3 more levels are each halved
        for (j in 2..4) {
            mLenByZoomLevel?.let {
                it.set(j, (it.get(j - 1) ?: 0) / 2)
                mValuesByZoomLevel?.set(j, DoubleArray(it.get(j)))
                mZoomFactorByZoomLevel[j] = mZoomFactorByZoomLevel[j - 1] / 2.0
                for (i in 0 until (it[j])) {
                    mValuesByZoomLevel?.get(j)?.set(i, 0.5 *
                            ((mValuesByZoomLevel?.get(j-1)?.get(2 * i) ?: 0.toDouble()) +
                                    (mValuesByZoomLevel?.get(j-1)?.get(2 * i+1) ?: 0.toDouble())))
                }
            }
        }
        mZoomLevel = if (numFrames > 5000) {
            3
        } else if (numFrames > 1000) {
            2
        } else if (numFrames > 300) {
            1
        } else {
            0
        }
        mInitialized = true
    }

    /**
     * Called the first time we need to draw when the zoom level has changed
     * or the screen is resized
     */
    private fun computeIntsForThisZoomLevel() {
        val halfHeight = measuredHeight / 2 - 1
        mLenByZoomLevel?.let {
            mHeightsAtThisZoomLevel = IntArray(it.get(mZoomLevel))
            for (i in 0 until it.get(mZoomLevel)) {
            mHeightsAtThisZoomLevel?.set(i, ((mValuesByZoomLevel?.get(mZoomLevel)?.get(i) ?: 0.toDouble()) * halfHeight).toInt())
        }

        }
    }

    interface WaveformListener {
        fun waveformTouchStart(x: Float)
        fun waveformTouchMove(x: Float)
        fun waveformTouchEnd()
        fun waveformFling(x: Float)
        fun waveformDraw()
        fun waveformZoomIn()
        fun waveformZoomOut()
    }

    init {

        // We don't want keys, the markers get these
        isFocusable = false
        val res = resources
        mGridPaint = Paint()
        mGridPaint.isAntiAlias = false
        mGridPaint.color = res.getColor(R.color.secoundryColor)
        mSelectedLinePaint = Paint()
        mSelectedLinePaint.isAntiAlias = false
        mSelectedLinePaint.color = res.getColor(R.color.recorded_waves_color)
        mUnselectedLinePaint = Paint()
        mUnselectedLinePaint.isAntiAlias = false
        mUnselectedLinePaint.color = res.getColor(R.color.recorded_waves_color)
        mUnselectedBkgndLinePaint = Paint()
        mUnselectedBkgndLinePaint.isAntiAlias = false
        mUnselectedBkgndLinePaint.color = res.getColor(R.color.secoundryColor)
        mBorderLinePaint = Paint()
        mBorderLinePaint.isAntiAlias = true
        mBorderLinePaint.strokeWidth = 6f
        mBorderLinePaint.pathEffect = DashPathEffect(floatArrayOf(3.0f, 2.0f), 0.0f)
        mBorderLinePaint.color = res.getColor(R.color.yellow_500)
        mPlaybackLinePaint = Paint()
        mPlaybackLinePaint.isAntiAlias = false
        mPlaybackLinePaint.strokeWidth = 3f
        mPlaybackLinePaint.color = res.getColor(R.color.recorded_current_index_line_color)
        mTimecodePaint = Paint()
        mTimecodePaint.textSize = 12f
        mTimecodePaint.isAntiAlias = true
        mTimecodePaint.color = res.getColor(R.color.secoundryColor)
        mTimecodePaint.setShadowLayer(2f, 1f, 1f, res.getColor(R.color.secoundryColor))
        mGestureDetector = GestureDetector(
                context,
                object : SimpleOnGestureListener() {
                    override fun onFling(e1: MotionEvent, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                        mListener?.waveformFling(vx)
                        return true
                    }
                }
        )
        mScaleGestureDetector = ScaleGestureDetector(
                context,
                object : SimpleOnScaleGestureListener() {
                    override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                        Log.v("Ringdroid", "ScaleBegin " + d.currentSpanX)
                        mInitialScaleSpan = Math.abs(d.currentSpanX)
                        return true
                    }

                    override fun onScale(d: ScaleGestureDetector): Boolean {
                        val scale = Math.abs(d.currentSpanX)
                        Log.v("Ringdroid", "Scale " + (scale - mInitialScaleSpan))
                        if (scale - mInitialScaleSpan > 40) {
                            mListener?.waveformZoomIn()
                            mInitialScaleSpan = scale
                        }
                        if (scale - mInitialScaleSpan < -40) {
                            mListener?.waveformZoomOut()
                            mInitialScaleSpan = scale
                        }
                        return true
                    }

                    override fun onScaleEnd(d: ScaleGestureDetector) {
                        Log.v("Ringdroid", "ScaleEnd " + d.currentSpanX)
                    }
                }
        )
        mSoundFile = null
        mLenByZoomLevel = null
        mValuesByZoomLevel = null
        mHeightsAtThisZoomLevel = null
        mOffset = 0
        mPlaybackPos = -1
        mSelectionStart = 0
        mSelectionEnd = 0
        mDensity = 1.0f
        mInitialized = false
    }
}