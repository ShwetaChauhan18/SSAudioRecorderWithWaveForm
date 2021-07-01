/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wave.audiorecording.audioplayer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.wave.audiorecording.R
import com.wave.audiorecording.util.AndroidUtils
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.IntArrayList
import com.wave.audiorecording.util.TimeUtils
import java.util.ArrayList
import java.util.LinkedList

class WaveformView : View {
    private val empty = IntArray(0)
    private var pxPerSecond = DEFAULT_PIXEL_PER_SECOND.toFloat()
    private var waveformPaint: Paint? = null

    //private Paint gridPaint;
    private var scrubberPaint: Paint? = null

    //private TextPaint textPaint;
    private var path = Path()
    private var waveformData: IntArray? = null
    private var playProgressPx = 0
    private var waveForm: IntArray? = null
    private var recordingData: MutableList<Int>? = null
    private var totalRecordingSize: Long = 0
    private var showRecording = false
    private var isInitialized = false
    private var textHeight = 0f
    private var inset = 0f
    private var prevScreenShift = 0
    private var startX = 0f
    private var readPlayProgress = true
    private var screenShift = 0
    private var waveformShift = 0
    private var viewWidth = 0

    /**
     * Defines which grid drawn for short waveform or for long.
     */
    private var isShortWaveForm = true

    /**
     * Values used to prevent call [.adjustWaveformHeights] before view is measured because
     * in that method used measured height value which calculates in [.onMeasure]
     */
    private var isMeasured = false
    private var onSeekListener: OnSeekListener? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        isFocusable = false
        recordingData = LinkedList()
        totalRecordingSize = 0
        path = Path()
        waveformPaint = Paint()
        waveformPaint?.style = Paint.Style.STROKE
        waveformPaint?.strokeWidth = AndroidUtils.dpToPx(1.2f)
        waveformPaint?.isAntiAlias = true
        waveformPaint?.color = context.resources.getColor(R.color.recording_waves_color)
        scrubberPaint = Paint()
        scrubberPaint?.isAntiAlias = false
        scrubberPaint?.style = Paint.Style.STROKE
        scrubberPaint?.strokeWidth = AndroidUtils.dpToPx(2)
        scrubberPaint?.color = context.resources.getColor(R.color.md_blue_gray_300)

        /*gridPaint = new Paint();
		gridPaint.setColor(context.getResources().getColor(R.color.md_blue_gray_500));
		gridPaint.setStrokeWidth(AndroidUtils.dpToPx(1)/2);*/textHeight = context.resources.getDimension(R.dimen.text_normal)
        inset = textHeight + PADD
        /*textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		textPaint.setColor(context.getResources().getColor(R.color.md_blue_gray_500));
		textPaint.setStrokeWidth(AndroidUtils.dpToPx(1));
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		textPaint.setTextSize(textHeight);*/playProgressPx = -1
        waveForm = null
        isInitialized = false
        setOnTouchListener { v, motionEvent ->
            if (!showRecording) {
                when (motionEvent.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        readPlayProgress = false
                        startX = motionEvent.x
                        if (onSeekListener != null) {
                            onSeekListener?.onStartSeek()
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        var shift = (prevScreenShift + motionEvent.x - startX).toInt()
                        //Right waveform move edge
                        if (shift <= -AndroidUtils.dpToPx(waveformData?.size ?: 0)) {
                            shift = (-AndroidUtils.dpToPx(waveformData?.size ?: 0)).toInt()
                        }
                        //Left waveform move edge
                        if (shift > 0) {
                            shift = 0
                        }
                        if (onSeekListener != null) {
                            onSeekListener?.onSeeking(-screenShift, AndroidUtils.convertPxToMills(-screenShift.toLong(), pxPerSecond).toLong())
                        }
                        playProgressPx = -shift
                        updateShifts(shift)
                        invalidate()
                    }
                    MotionEvent.ACTION_UP -> {
                        if (onSeekListener != null) {
                            onSeekListener?.onSeek(-screenShift, AndroidUtils.convertPxToMills(-screenShift.toLong(), pxPerSecond).toLong())
                        }
                        prevScreenShift = screenShift
                        readPlayProgress = true
                        performClick()
                    }
                }
            }
            true
        }
    }

    fun setPlayback(px: Int) {
        if (readPlayProgress) {
            playProgressPx = px
            updateShifts(-playProgressPx)
            prevScreenShift = screenShift
            invalidate()
        }
    }

    fun moveToStart() {
        val moveAnimator: ValueAnimator
        moveAnimator = ValueAnimator.ofInt(playProgressPx, 0)
        moveAnimator.interpolator = DecelerateInterpolator()
        moveAnimator.duration = ANIMATION_DURATION.toLong()
        moveAnimator.addUpdateListener { animation ->
            val moveVal = animation.animatedValue as Int
            setPlayback(moveVal)
        }
        moveAnimator.start()
    }

    /**
     * Rewinds current play position. (Current position + mills)
     *
     * @param mills time interval.
     */
    fun rewindMills(mills: Long) {
        playProgressPx += AndroidUtils.convertMillsToPx(mills, pxPerSecond)
        updateShifts(-playProgressPx)
        prevScreenShift = screenShift
        invalidate()
        if (onSeekListener != null) {
            onSeekListener?.onSeeking(-screenShift, AndroidUtils.convertPxToMills(-screenShift.toLong(), pxPerSecond).toLong())
        }
    }

    /**
     * Set new current play position in pixels.
     *
     * @param px value.
     */
    fun seekPx(px: Int) {
        playProgressPx = px
        updateShifts(-playProgressPx)
        prevScreenShift = screenShift
        invalidate()
        if (onSeekListener != null) {
            onSeekListener?.onSeeking(-screenShift, AndroidUtils.convertPxToMills(-screenShift.toLong(), pxPerSecond).toLong())
        }
    }

    fun setWaveform(frameGains: IntArray?) {
        if (frameGains != null) {
            waveForm = frameGains
            waveForm?.let {
                if (isMeasured) {
                    adjustWaveformHeights(it)
                }
            }
        } else {
            if (isMeasured) {
                adjustWaveformHeights(IntArray(0))
            }
        }
        requestLayout()
    }

    fun setPxPerSecond(pxPerSecond: Float) {
        isShortWaveForm = pxPerSecond == DEFAULT_PIXEL_PER_SECOND.toFloat()
        this.pxPerSecond = pxPerSecond
    }

    fun showRecording() {
        updateShifts((-AndroidUtils.dpToPx(totalRecordingSize.toFloat())).toInt())
        pxPerSecond = AndroidUtils.dpToPx(AppConstants.SHORT_RECORD_DP_PER_SECOND.toFloat())
        isShortWaveForm = true
        showRecording = true
        invalidate()
    }

    fun hideRecording() {
        showRecording = false
        updateShifts(0)
        clearRecordingData()
    }

    val waveformLength: Int
        get() = waveformData?.let {
            waveformData?.size
        } ?: 0

    fun addRecordAmp(amp: Int) {
        var amp = amp
        if (amp < 0) {
            amp = 0
        }
        totalRecordingSize++
        updateShifts((-AndroidUtils.dpToPx(totalRecordingSize.toFloat())).toInt())
        recordingData?.let {
            it.add(convertAmp(amp.toDouble()))
            if (it.size > AndroidUtils.pxToDp(viewWidth)) {
                it.removeAt(0)
            }
        }
        invalidate()
    }

    fun setRecordingData(data: IntArrayList?) {
        post {
            if (data != null) {
                recordingData?.clear()
                val count = AndroidUtils.pxToDp(viewWidth).toInt()
                if (data.size() > count) {
                    for (i in data.size() - count until data.size()) {
                        recordingData?.add(convertAmp(data[i].toDouble()))
                    }
                } else {
                    for (i in 0 until data.size()) {
                        recordingData?.add(convertAmp(data[i].toDouble()))
                    }
                }
                totalRecordingSize = data.size().toLong()
                updateShifts((-AndroidUtils.dpToPx(totalRecordingSize.toFloat())).toInt())
                invalidate()
            }
        }
    }

    /**
     * Convert dB amp value to view amp.
     */
    private fun convertAmp(amp: Double): Int {
        return (amp * ((measuredHeight / 2).toFloat() / 32767)).toInt()
    }

    fun clearRecordingData() {
        recordingData = ArrayList()
        totalRecordingSize = 0
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        if (selected) {
            waveformPaint?.color = context.resources.getColor(R.color.md_grey_500)
        } else {
            waveformPaint?.color = context.resources.getColor(R.color.md_grey_700)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!isMeasured) isMeasured = true
        // Reconcile the measured dimensions with the this view's constraints and
        // set the final measured viewWidth and height.
        val width = MeasureSpec.getSize(widthMeasureSpec)
        viewWidth = width
        screenShift = -playProgressPx
        waveformShift = screenShift + viewWidth / 2
        prevScreenShift = screenShift
        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (isMeasured && !isInitialized) {
            waveForm?.let {
                adjustWaveformHeights(it)
            } ?: adjustWaveformHeights(empty)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (waveformData == null && recordingData?.size == 0) {
            return
        }
        val measuredHeight = measuredHeight
        if (isShortWaveForm) {
            drawGrid(canvas)
        } else {
            drawGridForUpperWave(canvas)
        }
        if (showRecording) {
            drawRecordingWaveformForUpperWave(canvas)
        } else {
            drawWaveFormForUpperWave(canvas)
            val density = AndroidUtils.dpToPx(1)

            //Draw waveform start indication
            waveformPaint?.let {
                canvas.drawLine(waveformShift.toFloat(), inset, waveformShift.toFloat(), measuredHeight - inset, it)

                //Draw waveform end indication
                canvas.drawLine(waveformShift + (waveformData?.size ?: 0) * density, inset,
                        waveformShift + (waveformData?.size ?: 0) * density, measuredHeight - inset, it)

            }
        }
        scrubberPaint?.let { canvas.drawLine(0f, 0f, 0f, measuredHeight.toFloat(), it) }
    }

    private fun updateShifts(px: Int) {
        screenShift = px
        waveformShift = screenShift + viewWidth / 2
    }

    /**
     * Draws grid for short waveform shorter than [AppConstants.LONG_RECORD_THRESHOLD_SECONDS] value.
     * Also for recording.
     */
    private fun drawGrid(canvas: Canvas) {
        val height = height.toFloat()
        val count = 3 + viewWidth / DEFAULT_PIXEL_PER_SECOND
        val gridShift = waveformShift % (DEFAULT_PIXEL_PER_SECOND * 2)
        run {
            var i = -2f
            while (i < count) {

                //Draw seconds marks
                var xPos = i * DEFAULT_PIXEL_PER_SECOND + gridShift
                xPos = (i + 1) * DEFAULT_PIXEL_PER_SECOND + gridShift
                i += 2f
            }
        }
        var i = -2f
        while (i < count) {

            //Draw seconds marks
            val xPos = i * DEFAULT_PIXEL_PER_SECOND + gridShift
            val mills = ((-waveformShift / DEFAULT_PIXEL_PER_SECOND + gridShift / DEFAULT_PIXEL_PER_SECOND + i) * 1000).toLong()
            if (mills >= 0) {
                val text = TimeUtils.formatTimeIntervalMinSec(mills)
            }
            i += 2f
        }
    }

    /**
     * Draws grid for long waveform longer than [AppConstants.LONG_RECORD_THRESHOLD_SECONDS] value.
     */
    private fun drawGridForUpperWave(canvas: Canvas) {
        val height = height.toFloat()
        val markCount = AppConstants.GRID_LINES_COUNT
        val count = 3 + markCount
        val pxPerMark = viewWidth / markCount
        val secPerMark = pxPerMark / pxPerSecond
        val gridShift = waveformShift % (pxPerMark * 2)
        run {
            var i = -2f
            while (i < count) {

                //Draw seconds marks
                var xPos = i * pxPerMark + gridShift
                xPos = (i + 1) * pxPerMark + gridShift
                i += 2f
            }
        }
        var i = -2f
        while (i < count) {

            //Draw seconds marks
            val xPos = i * pxPerMark + gridShift
            val mills = ((-waveformShift / pxPerSecond + gridShift / pxPerSecond + secPerMark * i) * 1000).toLong()
            if (mills >= 0) {
                val text = TimeUtils.formatTimeIntervalMinSec(mills)
            }
            i += 2f
        }
    }

    private fun drawWaveForm(canvas: Canvas) {
        waveformData?.let {
            if ((it.size) > 0) {
                var width = it.size
                val half = measuredHeight / 2
                if (width > measuredWidth) {
                    width = measuredWidth
                }
                path.reset()
                var xPos = waveformShift.toFloat()
                if (xPos < VIEW_DRAW_EDGE) {
                    xPos = VIEW_DRAW_EDGE.toFloat()
                }
                path.moveTo(xPos, half.toFloat())
                path.lineTo(xPos, half.toFloat())
                val dpi = AndroidUtils.dpToPx(1)
                for (i in 1 until width) {
                    xPos = waveformShift + i * dpi
                    if (xPos > VIEW_DRAW_EDGE && xPos < viewWidth - VIEW_DRAW_EDGE) {
                        path.lineTo(xPos, (half - it[i]).toFloat())
                    }
                }
                for (i in width - 1 downTo 0) {
                    xPos = waveformShift + i * dpi
                    if (xPos > VIEW_DRAW_EDGE && xPos < viewWidth - VIEW_DRAW_EDGE) {
                        path.lineTo(xPos, (half + 1 + it[i]).toFloat())
                    }
                }
                xPos = waveformShift.toFloat()
                if (xPos < VIEW_DRAW_EDGE) {
                    xPos = VIEW_DRAW_EDGE.toFloat()
                }
                path.lineTo(xPos, half.toFloat())
                path.close()
                waveformPaint?.let { it1 -> canvas.drawPath(path, it1) }
            }
        }
    }

    private fun drawWaveFormForUpperWave(canvas: Canvas) {
        waveformData?.let {
            var width = it.size
            val half = measuredHeight / 2
            if (width > measuredWidth) {
                width = measuredWidth
            }
            val dpi = AndroidUtils.dpToPx(1)
            val lines = FloatArray(width * 4)
            var step = 0
            for (i in 0 until width) {
                lines[step] = waveformShift + i * dpi
                lines[step + 1] = (half + it[i] + 1).toFloat()
                lines[step + 2] = waveformShift + i * dpi
                lines[step + 3] = (half - it[i] - 1).toFloat()
                step += 4
            }
            waveformPaint?.let { it1 -> canvas.drawLines(lines, 0, lines.size, it1) }
        }
    }

    private fun drawRecordingWaveform(canvas: Canvas) {
        recordingData?.let {
            if (it.size > 0) {
                val half = measuredHeight / 2
                path.reset()
                var xPos = waveformShift.toFloat()
                if (xPos < VIEW_DRAW_EDGE) {
                    xPos = VIEW_DRAW_EDGE.toFloat()
                }
                path.moveTo(xPos, half.toFloat())
                path.lineTo(xPos, half.toFloat())
                val dpi = AndroidUtils.dpToPx(1)
                var startPos = 1
                if (waveformShift < 0) {
                    startPos = (waveformShift * dpi).toInt()
                }
                for (i in startPos until it.size) {
                    xPos = waveformShift + i * dpi
                    if (xPos > VIEW_DRAW_EDGE && xPos < viewWidth - VIEW_DRAW_EDGE) {
                        path.lineTo(xPos, (half - it[i]).toFloat())
                    }
                }
                for (i in it.size - 1 downTo startPos) {
                    xPos = waveformShift + i * dpi
                    if (xPos > VIEW_DRAW_EDGE && xPos < viewWidth - VIEW_DRAW_EDGE) {
                        path.lineTo(xPos, (half + 1 + it[i]).toFloat())
                    }
                }
                xPos = waveformShift.toFloat()
                if (xPos < VIEW_DRAW_EDGE) {
                    xPos = VIEW_DRAW_EDGE.toFloat()
                }
                path.lineTo(xPos, half.toFloat())
                path.close()
                waveformPaint?.let { it1 -> canvas.drawPath(path, it1) }
            }
        }
    }

    private fun drawRecordingWaveformForUpperWave(canvas: Canvas) {
        recordingData?.let {
            if (it.size > 0) {
                val width = it.size
                val half = measuredHeight / 2
                val dpi = AndroidUtils.dpToPx(1)
                val lines = FloatArray(width * 4)
                var step = 0
                for (i in 0 until width) {
                    lines[step] = viewWidth.toFloat() - i * dpi
                    lines[step + 1] = (half + it[it.size - 1 - i] + 1).toFloat()
                    lines[step + 2] = viewWidth.toFloat() - i * dpi
                    lines[step + 3] = (half - it[it.size - 1 - i] - 1).toFloat()
                    step += 4
                }
                waveformPaint?.let { it1 -> canvas.drawLines(lines, 0, lines.size, it1) }
            }
        }
    }

    /**
     * Called once when a new sound file is added
     */
    private fun adjustWaveformHeights(frameGains: IntArray) {
        val numFrames = frameGains.size
        //One frame corresponds one pixel on screen

        //Find the highest gain
        var maxGain = 1.0
        for (i in 0 until numFrames) {
            if (frameGains[i] > maxGain) {
                maxGain = frameGains[i].toDouble()
            }
        }
        // Make sure the range is no more than 0 - 255
        var scaleFactor = 1.0
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain
        }

        // Build histogram of 256 bins and figure out the new scaled max
        maxGain = 0.0
        val gainHist = IntArray(256)
        for (i in 0 until numFrames) {
            var smoothedGain = (frameGains[i] * scaleFactor).toInt()
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
        var range = maxGain - minGain
        if (range <= 0) {
            range = 1.0
        }
        for (i in 0 until numFrames) {
            var value = (frameGains[i] * scaleFactor - minGain) / range
            if (value < 0.0) value = 0.0
            if (value > 1.0) value = 1.0
            heights[i] = value * value
        }
        val halfHeight = measuredHeight / 2 - inset.toInt() - 1
        waveformData = IntArray(numFrames)
        for (i in 0 until numFrames) {
            waveformData?.set(i, (heights[i] * halfHeight).toInt())
        }
        isInitialized = true
    }

    fun setOnSeekListener(onSeekListener: OnSeekListener?) {
        this.onSeekListener = onSeekListener
    }

    interface OnSeekListener {
        fun onStartSeek()
        fun onSeek(px: Int, mills: Long)
        fun onSeeking(px: Int, mills: Long)
    }

    companion object {
        private val DEFAULT_PIXEL_PER_SECOND = AndroidUtils.dpToPx(AppConstants.SHORT_RECORD_DP_PER_SECOND.toFloat()).toInt()
        private val SMALL_LINE_HEIGHT = AndroidUtils.dpToPx(12)
        private val PADD = AndroidUtils.dpToPx(6)
        private const val VIEW_DRAW_EDGE = 0
        private const val ANIMATION_DURATION = 330 //mills.
    }
}