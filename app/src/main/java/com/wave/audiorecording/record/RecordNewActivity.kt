package com.wave.audiorecording.record

import android.Manifest
import android.animation.Animator
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.AlphabeticIndex
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.MaterialToolbar
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.R
import com.wave.audiorecording.app.PlaybackService
import com.wave.audiorecording.audioplayer.WaveformView
import com.wave.audiorecording.audioplayer.WaveformView.OnSeekListener
import com.wave.audiorecording.audioplayer.trim.MarkerView
import com.wave.audiorecording.audioplayer.trim.MarkerView.MarkerListener
import com.wave.audiorecording.audioplayer.trim.SamplePlayer
import com.wave.audiorecording.audioplayer.trim.SongMetadataReader
import com.wave.audiorecording.audioplayer.trim.SoundFile
import com.wave.audiorecording.audioplayer.trim.SoundFile.Companion.create
import com.wave.audiorecording.audioplayer.trim.WaveformViewTrim
import com.wave.audiorecording.audioplayer.trim.WaveformViewTrim.WaveformListener
import com.wave.audiorecording.model.RecordInfo
import com.wave.audiorecording.util.ActivityUtil
import com.wave.audiorecording.util.AndroidUtils
import com.wave.audiorecording.util.AnimationUtil
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.Constants
import com.wave.audiorecording.util.DeviceUtils
import com.wave.audiorecording.util.FileUtil
import com.wave.audiorecording.util.IntArrayList
import com.wave.audiorecording.util.TimeUtils
import com.wave.audiorecording.util.ViewUtils
import java.io.File
import java.io.RandomAccessFile

class RecordNewActivity : AppCompatActivity(), RecordContract.View, View.OnClickListener,
    MarkerListener, WaveformListener,
    Toolbar.OnMenuItemClickListener {
    private var waveformView: WaveformView? = null
    private var ivPlaceholder: TextView? = null
    private var btnRecord: ImageButton? = null
    private var btnImport: ImageButton? = null
    private var txtName: TextView? = null
    private val topAppBar: MaterialToolbar by lazy {
        ViewUtils.find(this, R.id.topAppBar)
    }
    private val menuList: Menu by lazy {
        topAppBar.menu
    }
    private val downloadMenuItem: MenuItem by lazy {
        menuList.findItem(R.id.menuDownload)
    }
    private val restoreMenuItem: MenuItem by lazy {
        menuList.findItem(R.id.menuRestore)
    }
    private var lnrLayoutRecord: LinearLayout? = null
    private var btnRecordStop: ImageView? = null
    private var txtProgress: TextView? = null
    private var btnRecordPause: ImageView? = null

    private var btnPlay: LinearLayout? = null
    private var btnImgPlay: ImageButton? = null

    private var deleteRecord = false
    private var deleteAlreadyRecordAudio = false
    private var mIsImportRecord = false
    private var mIsFromPauseRecord = false
    private var mIsFromInit = false
    private var mIsFromFile = false
    private var mIsMarkerTouch = false
    private var mIsDataNotNull = false
    private var markerStart: MarkerView? = null
    private var markerEnd: MarkerView? = null
    private var audioWaveform: WaveformViewTrim? = null
    private var mPlayer: SamplePlayer? = null
    private var txtStartPosition: TextView? = null
    private var txtEndPosition: TextView? = null
    private var relativeLayoutTrim: LinearLayout? = null
    private var mLoadedSoundFile: SoundFile? = null
    private var mHandler: Handler? = null
    private var mTouchDragging = false
    private var mTouchStart = 0f
    private var mTouchInitialOffset = 0
    private var mTouchInitialStartPos = 0
    private var mTouchInitialEndPos = 0
    private var mDensity = 0f
    private var mMarkerLeftInset = 0
    private var mMarkerRightInset = 0
    private var mMarkerTopOffset = 0
    private var mMarkerBottomOffset = 0
    private var mTextLeftInset = 0
    private var mTextRightInset = 0
    private var mTextTopOffset = 0
    private var mTextBottomOffset = 0
    private var mOffset = 0
    private var mOffsetGoal = 0
    private var mFlingVelocity = 0
    private var mPlayEndMillSec = 0
    private var mWidth = 0
    private var mMaxPos = 0
    private var mStartPos = 0
    private var mEndPos = 0
    private var mStartVisible = false
    private var mEndVisible = false
    private var mLastDisplayedStartPos = 0
    private var mLastDisplayedEndPos = 0
    private var mIsPlaying = false
    private var mKeyDown = false
    private var mLoadingLastUpdateTime: Long = 0
    private var mLoadingKeepGoing = false
    private var mFile: File? = null
    private var progressbarTxt: TextView? = null
    private var audioButtonColor: String? = "0"
    private var audioRecordedPath: String? = ""
    private var presenter: RecordContract.UserActionsListener? = null
    private val mTimerRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mStartPos != mLastDisplayedStartPos) {
                txtStartPosition?.text = formatTime(mStartPos)
                mLastDisplayedStartPos = mStartPos
            }
            if (mEndPos != mLastDisplayedEndPos) {
                txtEndPosition?.text = formatTime(mEndPos)
                mLastDisplayedEndPos = mEndPos
            }
            mHandler?.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        if (intent != null && intent.extras != null && intent.extras?.containsKey(Constants.Intents.ELEMENT_AUDIO_BUTTON_BG_THEME_COLOR) == true) {
            audioButtonColor =
                intent.getStringExtra(Constants.Intents.ELEMENT_AUDIO_BUTTON_BG_THEME_COLOR)
            if (intent.extras?.containsKey(Constants.Intents.RECORDED_AUDIO_PATH) == true) {
                audioRecordedPath = intent.getStringExtra(Constants.Intents.RECORDED_AUDIO_PATH)
            }
        }
        ActivityUtil.makeStatusBarColor(this, R.color.white, false)
        ActivityUtil.makeStatusBarBlack(this)
        init()
        initTrim()
        initVisibleBtn()
        initWave()
        //        ThemeUtils.setButtonDrawableColours(btnRecord, audioButtonColor);
    }

    private fun init() {
        mIsFromInit = true
        waveformView = ViewUtils.find(this, R.id.record)
        ivPlaceholder = ViewUtils.find(this, R.id.placeholder)
        btnRecord = ViewUtils.find(this, R.id.btn_record)
        btnImport = ViewUtils.find(this, R.id.btn_import)
        txtName = ViewUtils.find(this, R.id.txt_name)
        lnrLayoutRecord = ViewUtils.find(this, R.id.lnr_layout_record)
        btnRecordStop = ViewUtils.find(this, R.id.btn_record_stop)
        txtProgress = ViewUtils.find(this, R.id.txt_progress)
        btnRecordPause = ViewUtils.find(this, R.id.btn_record_pause)
        btnPlay = ViewUtils.find(this, R.id.btn_play)
        btnImgPlay = ViewUtils.find(this, R.id.img_button_play)
        progressbarTxt = ViewUtils.find(this, R.id.progressbarTxt)
        txtProgress?.text = TimeUtils.formatTimeIntervalHourMinSec2(0)
        btnRecordPause?.visibility = View.INVISIBLE
        btnRecordPause?.isEnabled = false
        btnRecord?.visibility = View.VISIBLE
        btnRecord?.isEnabled = true
        topAppBar.setOnMenuItemClickListener(this)
        topAppBar.setNavigationOnClickListener {
            if (mPlayer != null && mPlayer?.isPlaying() == true) {
                mIsPlaying = false
                mPlayer?.pause()
            }
            presenter?.pausePlayback()
            deleteRecord = true
            presenter?.stopRecording(true)
            initTrim()
            initVisibleBtn()
            initWave()
        }
        btnPlay?.setOnClickListener(this)
        btnImgPlay?.setOnClickListener(this)
        btnRecordStop?.setOnClickListener(this)
        btnRecord?.setOnClickListener(this)
        btnRecordPause?.setOnClickListener(this)
        btnImport?.setOnClickListener(this)
        txtName?.setOnClickListener(this)
    }

    private fun initTrim() {
        mHandler = Handler()
        markerStart = ViewUtils.find(this, R.id.markerStart)
        markerEnd = ViewUtils.find(this, R.id.markerEnd)
        audioWaveform = ViewUtils.find(this, R.id.audioWaveform)
        txtStartPosition = ViewUtils.find(this, R.id.txtStartPosition)
        txtEndPosition = ViewUtils.find(this, R.id.txtEndPosition)
        relativeLayoutTrim = ViewUtils.find(this, R.id.relativeLayoutTrim)
        mKeyDown = false
        audioWaveform?.setListener(this)
        markerStart?.setListener(this)
        markerStart?.alpha = 1f
        markerStart?.isFocusable = true
        markerStart?.isFocusableInTouchMode = true
        mStartVisible = true
        markerEnd?.setListener(this)
        markerEnd?.alpha = 1f
        markerEnd?.isFocusable = true
        markerEnd?.isFocusableInTouchMode = true
        mEndVisible = true
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mDensity = metrics.density
        Log.e("density", mDensity.toString() + "")
        /**
         * Change this for marker handle as per your view
         */
        mMarkerLeftInset = 2
        mMarkerRightInset = (18 * mDensity).toInt()
        if (DeviceUtils.isTabletDevice(this)) {
            mMarkerLeftInset = 0
            mMarkerRightInset = (22 * mDensity).toInt()
        }
        markerStart?.let { newMarkerStart ->
            audioWaveform?.post {
                val layoutParams = audioWaveform?.layoutParams as RelativeLayout.LayoutParams
                layoutParams.height = (newMarkerStart.measuredHeight)
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                audioWaveform?.layoutParams = layoutParams
                Log.e("TAG1", audioWaveform?.measuredHeight.toString() + "")
            }
            relativeLayoutTrim?.let {
                it.post {
                    Log.e("TAG1", it.measuredHeight.toString() + "")
                    mMarkerTopOffset = ((it.measuredHeight / 2) - (newMarkerStart.measuredHeight))
                    mMarkerBottomOffset =
                        ((it.measuredHeight / 2) - (newMarkerStart.measuredHeight))
                }
            }
        }

        /*
         * Change this for duration text as per your view
         */
        mTextLeftInset = (20 * mDensity).toInt()
        mTextTopOffset = (-1 * mDensity).toInt()
        mTextRightInset = (19 * mDensity).toInt()
        mTextBottomOffset = (-40 * mDensity).toInt()
    }

    private fun initWave() {
        deleteRecord = false
        presenter = AudioRecordingWavesApplication.injector?.provideMainPresenter()
        presenter?.executeFirstRun()
        waveformView?.setOnSeekListener(object : OnSeekListener {
            override fun onStartSeek() {
                presenter?.disablePlaybackProgressListener()
            }

            override fun onSeek(px: Int, mills: Long) {
                presenter?.enablePlaybackProgressListener()
                presenter?.seekPlayback(px)
                val length = waveformView?.waveformLength
                txtProgress?.text = TimeUtils.formatTimeIntervalHourMinSec2(mills)
            }

            override fun onSeeking(px: Int, mills: Long) {
                val length = waveformView?.waveformLength
                txtProgress?.text = TimeUtils.formatTimeIntervalHourMinSec2(mills)
            }
        })
        waveformView?.clearRecordingData()
    }

    private fun initVisibleBtn() {
        Log.e("TAG", "initVisibleBtn")
        setNavigationCloseIconVisibility(false)
        downloadMenuItem.isVisible = true
        downloadMenuItem.isEnabled = true
        btnRecordPause?.visibility = View.INVISIBLE
        btnRecordPause?.isEnabled = false
        AnimationUtil.pauseButtonAnimation(btnRecordPause, false)
        btnRecord?.visibility = View.VISIBLE
        btnRecord?.isEnabled = true
        AnimationUtil.animation(btnRecord)
        btnImport?.visibility = View.VISIBLE
        btnImport?.isEnabled = true
        btnPlay?.visibility = View.GONE
        btnPlay?.isEnabled = false
        setDownloadIconVisibility(false)
        setIsDownloadIconEnable(false)
        lnrLayoutRecord?.visibility = View.INVISIBLE
        ivPlaceholder?.visibility = View.VISIBLE
        relativeLayoutTrim?.visibility = View.INVISIBLE
        btnRecordStop?.isEnabled = true
    }

    private fun setNavigationCloseIconVisibility(isVisible: Boolean) {
        if (isVisible) {
            topAppBar.setNavigationIcon(R.drawable.ic_close)
        } else
            topAppBar.setNavigationIcon(null)
    }

    private fun setDownloadIconVisibility(isVisible: Boolean) {
        downloadMenuItem.isVisible = isVisible
        setRestoreIconVisibility(isVisible)
    }

    private fun setIsDownloadIconEnable(isEnable: Boolean) {
        downloadMenuItem.isEnabled = isEnable
        setIsRestoreIconEnable(isEnable)
    }

    private fun setRestoreIconVisibility(isVisible: Boolean) {
        restoreMenuItem.isVisible = isVisible
    }

    private fun setIsRestoreIconEnable(isEnable: Boolean) {
        restoreMenuItem.isEnabled = isEnable
    }

    override fun onResume() {
        super.onResume()
        Log.e("TAG", "onResume")
    }

    override fun onStart() {
        super.onStart()
        Log.e("TAG", "onStart")
        presenter?.bindView(this)
        presenter?.setAudioRecorder(AudioRecordingWavesApplication.injector?.provideAudioRecorder())
        presenter?.updateRecordingDir(applicationContext)
        if (mIsFromInit) {
            mIsFromInit = false
            presenter?.clearDatabase()
            initVisibleBtn()
            presenter?.loadActiveRecord()
            if (!TextUtils.isEmpty(audioRecordedPath)) {
                loadFromFile(File(audioRecordedPath))
            }
        } else if (mLoadedSoundFile == null) {
            mIsFromFile = false
            presenter?.loadActiveRecord()
        } else if (!mIsFromFile) {
            waveformView?.visibility = View.INVISIBLE
            relativeLayoutTrim?.visibility = View.VISIBLE
            audioWaveform?.visibility = View.VISIBLE
            markerStart?.visibility = View.VISIBLE
            markerEnd?.visibility = View.VISIBLE
        } else {
            waveformView?.visibility = View.INVISIBLE
            ivPlaceholder?.visibility = View.INVISIBLE
            relativeLayoutTrim?.visibility = View.VISIBLE
            progressbarTxt?.visibility = View.VISIBLE
            btnRecord?.isEnabled = false
            btnImport?.isEnabled = false
            btnPlay?.isEnabled = false
            btnImgPlay?.isEnabled = false
            setIsDownloadIconEnable(false)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.e("TAG", "onStop")
        if (presenter != null) {
            if (mPlayer != null && mPlayer?.isPlaying() == true) {
                mIsPlaying = false
                mPlayer?.pause()
            }
            presenter?.unbindView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("TAG", "onDestroy")
        if (presenter != null) {
            initVisibleBtn()
            presenter?.stopRecording(true)
            presenter?.clearDatabase()
            presenter?.unbindView()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_record_pause, R.id.btn_record -> if (checkRecordPermission2() && checkStoragePermission2()) {
                //Start or stop recording
                presenter?.startRecording(applicationContext)
            }
            R.id.btn_import -> if (checkStoragePermissionImport()) {
                btnImport?.isEnabled = false
                startFileSelector()
            }
            R.id.btn_record_stop -> {
                btnPlay?.visibility = View.VISIBLE
                btnRecord?.visibility = View.GONE
                lnrLayoutRecord?.visibility = View.GONE
                btnPlay?.isEnabled = false
                btnImgPlay?.isEnabled = false
                setDownloadIconVisibility(true)
                setIsDownloadIconEnable(false)
                mIsDataNotNull = true
                presenter?.stopRecording(false)
            }
            R.id.img_button_play, R.id.btn_play ->                 //This method Starts or Pause playback.
                onPlay(mStartPos, mIsMarkerTouch)
        }
    }

    private fun startFileSelector() {
        mIsFromFile = true
        val intent_upload = Intent()
        intent_upload.type = "audio/*"
        intent_upload.addCategory(Intent.CATEGORY_OPENABLE)
        intent_upload.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(intent_upload, REQ_CODE_IMPORT_AUDIO)
    }

    private fun finishThisActivity() {
        if (deleteAlreadyRecordAudio) {
            val conData = Bundle()
            conData.putString(Constants.Intents.RETURN_FILENAME, Constants.EMPTY_FILE_NAME)
            val intent = intent
            intent.putExtras(conData)
            setResult(RESULT_OK, intent)
        }
        finish()
        overridePendingTransition(R.anim.no_animation, R.anim.slide_down_animation)
    }

    override fun onBackPressed() {
        if (mPlayer != null && mPlayer?.isPlaying() == true) {
            mPlayer?.pause()
        }
        finishThisActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        btnImport?.isEnabled = true
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_IMPORT_AUDIO && resultCode == RESULT_OK) {
            if (data?.data != null) {
                mIsDataNotNull = true
                isFromImportFileUpload
                data.data?.let {
                    val importFile = FileUtil.copyFile(this, it)
                    importFile?.let { loadFromFile(it) }
                }
            }
        }
    }

    private val isFromImportFileUpload: Unit
        private get() {
            waveformView?.visibility = View.INVISIBLE
            ivPlaceholder?.visibility = View.GONE
            relativeLayoutTrim?.visibility = View.VISIBLE
            progressbarTxt?.visibility = View.VISIBLE
            audioWaveform?.visibility = View.VISIBLE
            btnPlay?.isEnabled = false
            btnImgPlay?.isEnabled = false
            setIsDownloadIconEnable(false)
        }

    override fun keepScreenOn(on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun showRecordingStart() {
        Log.e("TAG", "showRecordingStart")
        if (!deleteRecord) {
            Log.e("TAG", "deleteRecord showRecordingStart")
            btnRecord?.visibility = View.INVISIBLE
            btnImport?.visibility = View.INVISIBLE
            btnImport?.isEnabled = false
            setNavigationCloseIconVisibility(true)
            lnrLayoutRecord?.let {
                it.post {
                    val cx = (it.width) / 2
                    val cy = (it.height) / 2
                    // get the final radius for the clipping circle
                    val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                    val anim = ViewAnimationUtils.createCircularReveal(it, cx, cy, 0f, finalRadius)
                    it.visibility = View.VISIBLE
                    if (!mIsFromPauseRecord) {
                        anim.start()
                    }
                }
            }
            btnRecordStop?.setImageResource(R.drawable.ic_pause)
            btnRecordPause?.visibility = View.VISIBLE
            btnRecordPause?.isEnabled = true
            AnimationUtil.pauseButtonAnimation(btnRecordPause, false)
            txtName?.isClickable = false
            txtName?.isFocusable = false
            txtName?.setCompoundDrawables(null, null, null, null)
            txtName?.visibility = View.INVISIBLE
            txtName?.setText(R.string.recording_progress)
            btnPlay?.visibility = View.INVISIBLE
            btnPlay?.isEnabled = false
            setDownloadIconVisibility(false)
            setIsDownloadIconEnable(false)
            waveformView?.showRecording()
            waveformView?.visibility = View.VISIBLE
            ivPlaceholder?.visibility = View.GONE
        }
    }

    override fun showRecordingStop() {
        Log.e("TAG", "showRecordingStop")
        if (!deleteRecord) {
            afterRecordingStopBtnVisibility()
            waveformView?.hideRecording()
            waveformView?.clearRecordingData()
        }
    }

    private fun afterRecordingStopBtnVisibility() {
        Log.e("TAG", "afterRecordingStopBtnVisibility")
        txtName?.isClickable = true
        txtName?.isFocusable = true
        txtName?.text = ""
        txtName?.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            getDrawable(R.drawable.ic_pencil_small),
            null
        )
        txtName?.visibility = View.INVISIBLE
        btnRecordStop?.setImageResource(R.drawable.ic_pause_blue)
        btnRecord?.visibility = View.INVISIBLE
        btnImport?.visibility = View.INVISIBLE
        btnImport?.isEnabled = false
        btnPlay?.visibility = View.VISIBLE
        btnPlay?.isEnabled = true
        setDownloadIconVisibility(true)
        setNavigationCloseIconVisibility(false)
        btnRecordPause?.visibility = View.INVISIBLE
        btnRecordPause?.isEnabled = false
        AnimationUtil.pauseButtonAnimation(btnRecordPause, false)
        lnrLayoutRecord?.visibility = View.INVISIBLE
    }

    override fun showRecordingPause() {
        Log.e("TAG", "showRecordingPause")
        if (!deleteRecord) {
            Log.e("TAG", "deleteRecord showRecordingPause")
            mIsFromPauseRecord = true
            btnRecord?.visibility = View.INVISIBLE
            btnImport?.visibility = View.INVISIBLE
            btnImport?.isEnabled = false
            setNavigationCloseIconVisibility(true)
            lnrLayoutRecord?.visibility = View.VISIBLE
            btnRecordStop?.setImageResource(R.drawable.ic_pause_blue)
            btnRecordPause?.visibility = View.VISIBLE
            btnRecordPause?.isEnabled = true
            AnimationUtil.pauseButtonAnimation(btnRecordPause, true)
            btnImport?.visibility = View.INVISIBLE
            btnImport?.isEnabled = false
            txtName?.isClickable = false
            txtName?.isFocusable = false
            txtName?.setCompoundDrawables(null, null, null, null)
            txtName?.setText(R.string.recording_paused)
            txtProgress?.setTextColor(resources.getColor(R.color.text_primary_light))
            txtName?.visibility = View.INVISIBLE
            setNavigationCloseIconVisibility(true)
            btnPlay?.visibility = View.INVISIBLE
            btnPlay?.isEnabled = false
            setDownloadIconVisibility(false)
            setIsDownloadIconEnable(false)
            waveformView?.visibility = View.VISIBLE
            ivPlaceholder?.visibility = View.GONE
        }
    }

    override fun onRecordingProgress(mills: Long, amp: Int) {
        txtProgress?.text = TimeUtils.formatTimeIntervalHourMinSec2(mills)
        waveformView?.addRecordAmp(amp)
    }

    override fun askRecordingNewName(id: Long, file: File) {
        setRecordName(id, file, true)
    }

    override fun startRecordingService() {}
    override fun stopRecordingService() {}
    override fun startPlaybackService(name: String?) {
        PlaybackService.startServiceForeground(applicationContext, name)
    }

    override fun showPlayStart(animate: Boolean) {
        btnRecordStop?.isEnabled = false
        if (animate) {
            AnimationUtil.viewAnimationX(btnImgPlay, 0f, object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    btnImgPlay?.setImageResource(R.drawable.ic_pause_small)
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        } else {
            btnImgPlay?.translationX = 0f
            txtProgress?.setTextColor(resources.getColor(R.color.md_red_500))
            btnImgPlay?.setImageResource(R.drawable.ic_pause_small)
        }
    }

    override fun showPlayPause() {
        audioWaveform?.setPlayback(
            audioWaveform?.millisecsToPixels(
                mPlayer?.getCurrentPosition()
                    ?: 0
            ) ?: 0
        )
        btnImgPlay?.setImageResource(R.drawable.ic_play)
    }

    override fun showPlayStop() {
        btnImgPlay?.setImageResource(R.drawable.ic_play)
        waveformView?.moveToStart()
        btnRecordStop?.isEnabled = true
        txtProgress?.text = TimeUtils.formatTimeIntervalHourMinSec2(0)
        AnimationUtil.viewAnimationX(btnImgPlay, 0f, object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    override fun onPlayProgress(mills: Long, px: Int, percent: Int) {
        waveformView?.setPlayback(px)
        txtProgress?.text = TimeUtils.formatTimeIntervalHourMinSec2(mills)
    }

    override fun showImportStart() {
        btnImport?.visibility = View.INVISIBLE
        btnPlay?.visibility = View.VISIBLE
        setDownloadIconVisibility(true)
        btnRecord?.visibility = View.GONE
        lnrLayoutRecord?.visibility = View.GONE
        mIsImportRecord = true
    }

    override fun hideImportProgress() {}
    override fun showOptionsMenu() {
        if (!mIsDataNotNull) {
            showRecordingPause()
        } else {
            mIsDataNotNull = false
        }
    }

    override fun hideOptionsMenu() {
        if (mIsDataNotNull) {
            waveformView?.visibility = View.INVISIBLE
            ivPlaceholder?.visibility = View.INVISIBLE
            relativeLayoutTrim?.visibility = View.VISIBLE
            progressbarTxt?.visibility = View.VISIBLE
            btnRecord?.isEnabled = false
            btnImport?.isEnabled = false
            btnPlay?.isEnabled = false
            btnImgPlay?.isEnabled = false
            setIsDownloadIconEnable(false)
        } else if (!mIsFromFile) initVisibleBtn()
    }

    override fun showRecordProcessing() {}
    override fun hideRecordProcessing() {}
    override fun showWaveForm(waveForm: IntArray, duration: Long) {
        if (waveForm.size > 0) {
            ivPlaceholder?.visibility = View.GONE
        } else {
            ivPlaceholder?.visibility = View.VISIBLE
            waveformView?.visibility = View.INVISIBLE
        }
        waveformView?.setWaveform(waveForm)
        waveformView?.setPxPerSecond(
            AndroidUtils.dpToPx(
                AudioRecordingWavesApplication.getDpPerSecond(
                    duration.toFloat() / 1000000f
                )
            )
        )
    }

    override fun waveFormToStart() {
        waveformView?.seekPx(0)
    }

    override fun showDuration(duration: String?) {}
    override fun showRecordingProgress(progress: String?) {
        txtProgress?.text = progress
    }

    override fun showName(name: String?) {
        if (name == null || name.isEmpty()) {
            txtName?.visibility = View.INVISIBLE
        } else if (txtName?.visibility == View.INVISIBLE) {
            txtName?.visibility = View.INVISIBLE
        }
        txtName?.text = name
    }

    override fun askDeleteRecord(name: String?) {
        AndroidUtils.showSimpleDialog(
            this@RecordNewActivity,
            R.drawable.ic_delete_forever,
            R.string.warning,
            applicationContext.getString(R.string.delete_record, name),
            { dialog, which -> presenter?.deleteActiveRecord(false) }
        )
    }

    override fun askDeleteRecordForever() {
        AndroidUtils.showSimpleDialog(
            this@RecordNewActivity,
            R.drawable.ic_delete_forever,
            R.string.warning,
            applicationContext.getString(R.string.delete_this_record),
            { dialog, which ->
                deleteRecord = true
                presenter?.stopRecording(true)
            }
        )
    }

    override fun deleteRecord() {
        if (deleteRecord) {
            initVisibleBtn()
            presenter?.clearDatabase()
            waveformView?.clearRecordingData()
            deleteRecord = false
        }
    }

    override fun showRecordInfo(info: RecordInfo?) {}
    override fun showRecordFile(file: File) {
        if (!deleteRecord) {
            waveformView?.visibility = View.INVISIBLE
            relativeLayoutTrim?.visibility = View.VISIBLE
            loadFromFile(file)
        }
    }

    override fun updateRecordingView(data: IntArrayList?) {
        waveformView?.showRecording()
        waveformView?.setRecordingData(data)
    }

    override fun showRecordsLostMessage(list: List<AlphabeticIndex.Record<*>?>?) {}
    override fun showProgress() {
        Log.e("TAG", "showProgress")
    }

    override fun hideProgress() {
        Log.e("TAG", "hideProgress")
        if (mIsImportRecord) {
            afterRecordingStopBtnVisibility()
            mIsImportRecord = false
        }
    }

    override fun showError(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    override fun showError(resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_LONG).show()
    }

    override fun showMessage(resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_LONG).show()
    }

    fun setRecordName(recordId: Long, file: File, showCheckbox: Boolean) {
        //Create dialog layout programmatically.
        val container = LinearLayout(applicationContext)
        container.orientation = LinearLayout.VERTICAL
        val containerLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        container.layoutParams = containerLp
        val editText = EditText(applicationContext)
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        editText.layoutParams = lp
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.length > AppConstants.MAX_RECORD_NAME_LENGTH) {
                    s.delete(s.length - 1, s.length)
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
        editText.setTextColor(resources.getColor(R.color.text_primary_light))
        editText.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.text_medium)
        )
        val pad = resources.getDimension(R.dimen.spacing_normal).toInt()
        val params = MarginLayoutParams(editText.layoutParams)
        params.setMargins(pad, pad, pad, pad)
        editText.layoutParams = params
        container.addView(editText)
        val fileName = FileUtil.removeFileExtension(file.name)
        editText.setText(fileName)
        val newName = editText.text.toString()
        if (!fileName.equals(newName, ignoreCase = true)) {
            presenter?.renameRecord(recordId, newName)
        }
    }

    private fun checkStoragePermissionImport(): Boolean {
        if (presenter?.isStorePublic == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ),
                        REQ_CODE_READ_EXTERNAL_STORAGE_IMPORT
                    )
                    return false
                }
            }
        }
        return true
    }

    private fun checkStoragePermissionPlayback(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK
                )
                return false
            }
        }
        return true
    }

    private fun checkRecordPermission2(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_CODE_RECORD_AUDIO)
                return false
            }
        }
        return true
    }

    private fun checkStoragePermission2(): Boolean {
        if (presenter?.isStorePublic == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    AndroidUtils.showDialog(
                        this, R.string.warning, R.string.need_write_permission,
                        {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ),
                                REQ_CODE_WRITE_EXTERNAL_STORAGE
                            )
                        }, null
                    )
                    return false
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQ_CODE_REC_AUDIO_AND_WRITE_EXTERNAL && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            visibleViewsAfterPermissionGranted()
        } else if (requestCode == REQ_CODE_RECORD_AUDIO && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (checkStoragePermission2()) {
                visibleViewsAfterPermissionGranted()
            }
        } else if (requestCode == REQ_CODE_WRITE_EXTERNAL_STORAGE && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            if (checkRecordPermission2()) {
                visibleViewsAfterPermissionGranted()
            }
        } else if (requestCode == REQ_CODE_READ_EXTERNAL_STORAGE_IMPORT && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            startFileSelector()
        } else if (requestCode == REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            presenter?.startPlayback()
        } else if (requestCode == REQ_CODE_WRITE_EXTERNAL_STORAGE && grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_DENIED
                    || grantResults[1] == PackageManager.PERMISSION_DENIED)
        ) {
            presenter?.setStoragePrivate(applicationContext)
            visibleViewsAfterPermissionGranted()
        }
    }

    private fun visibleViewsAfterPermissionGranted() {
        btnRecord?.visibility = View.GONE
        lnrLayoutRecord?.visibility = View.VISIBLE
        presenter?.startRecording(applicationContext)
    }

    private fun loadFromFile(record: File) {
        Log.e("record path", record.path)
        mFile = record
        val metadataReader = mFile?.name?.let { SongMetadataReader(this, it) }
        val mTitle = metadataReader?.mTitle
        val mArtist = metadataReader?.mArtist
        var titleLabel = mTitle
        if (mArtist != null && mArtist.length > 0) {
            titleLabel += " - $mArtist"
        }
        title = titleLabel
        mLoadingLastUpdateTime = AndroidUtils.currentTime
        mLoadingKeepGoing = true
        ivPlaceholder?.visibility = View.INVISIBLE
        waveformView?.visibility = View.INVISIBLE
        relativeLayoutTrim?.visibility = View.VISIBLE
        AndroidUtils.runOnUIThread({ progressbarTxt?.visibility = View.VISIBLE })
        val listener: SoundFile.ProgressListener = object : SoundFile.ProgressListener {
            override fun reportProgress(fractionComplete: Double): Boolean {
                val now = AndroidUtils.currentTime
                if (now - mLoadingLastUpdateTime > 100) {
                    mLoadingLastUpdateTime = now
                }
                return mLoadingKeepGoing
            }
        }

        // Load the sound file in a background thread
        val mLoadSoundFileThread: Thread = object : Thread() {
            override fun run() {
                try {
                    mLoadedSoundFile = create(mFile?.absolutePath, listener)
                    if (mLoadedSoundFile == null) {
                        val name = mFile?.name?.toLowerCase()
                        val components = name?.split("\\.".toRegex())?.toTypedArray()
                        val err: String
                        err = if ((components?.size ?: 0) < 2) {
                            "No Extension"
                        } else {
                            "Bad Extension"
                        }
                        Log.e(" >> ", "" + err)
                        return
                    }
                    mLoadedSoundFile?.let {
                        mPlayer = SamplePlayer(it)
                    }
                } catch (e: Exception) {
                    AndroidUtils.runOnUIThread({ progressbarTxt?.visibility = View.INVISIBLE })
                    e.printStackTrace()
                    return
                }
                AndroidUtils.runOnUIThread({ progressbarTxt?.visibility = View.INVISIBLE })
                if (mLoadingKeepGoing) {
                    val runnable = Runnable {
                        relativeLayoutTrim?.visibility = View.INVISIBLE
                        audioWaveform?.visibility = View.INVISIBLE
                        audioWaveform?.setBackgroundColor(resources.getColor(R.color.marker_color_alpha))
                        audioWaveform?.setIsDrawBorder(false)
                        mLoadedSoundFile?.let {
                            finishOpeningSoundFile(it, 0)
                        }
                    }
                    mHandler?.post(runnable)
                }
            }
        }
        mLoadSoundFileThread.start()
    }

    /**
     * After recording finish do necessary steps
     *
     * @param mSoundFile sound file
     * @param isReset    isReset
     */
    private fun finishOpeningSoundFile(mSoundFile: SoundFile, isReset: Int) {
        mIsFromFile = false
        afterRecordingStopBtnVisibility()
        waveformView?.visibility = View.INVISIBLE
        relativeLayoutTrim?.visibility = View.VISIBLE
        audioWaveform?.visibility = View.VISIBLE
        markerStart?.visibility = View.VISIBLE
        markerEnd?.visibility = View.VISIBLE
        audioWaveform?.setSoundFile(mSoundFile)
        audioWaveform?.recomputeHeights(mDensity)
        setIsDownloadIconEnable(true)
        btnImgPlay?.isEnabled = true
        btnPlay?.isEnabled = true
        mMaxPos = audioWaveform?.maxPos() ?: 0
        mLastDisplayedStartPos = -1
        mLastDisplayedEndPos = -1
        mTouchDragging = false
        mOffset = 0
        mOffsetGoal = 0
        mFlingVelocity = 0
        resetPositions()
        if (mEndPos > mMaxPos) mEndPos = mMaxPos
        if (isReset == 1) {
            mStartPos = audioWaveform?.secondsToPixels(0.0) ?: 0
            mEndPos = audioWaveform?.secondsToPixels(
                audioWaveform?.pixelsToSeconds(mMaxPos)
                    ?: 0.toDouble()
            ) ?: 0
        }
        if (audioWaveform != null && audioWaveform?.isInitialized() == true) {
            val seconds = audioWaveform?.pixelsToSeconds(mMaxPos) ?: 0.toDouble()
            val min = (seconds / 60).toInt()
            val sec = (seconds - 60 * min).toFloat()
        }
        updateDisplay()
    }

    @Synchronized
    private fun updateDisplay() {
        if (mIsPlaying) {
            val now = mPlayer?.getCurrentPosition() ?: 0
            val frames = audioWaveform?.millisecsToPixels(now) ?: 0
            audioWaveform?.setPlayback(frames)
            Log.e("mWidth >> ", "" + mWidth)
            setOffsetGoalNoUpdate(frames - mWidth / 2)
            if (now >= mPlayEndMillSec) {
                handlePause(true)
            }
        }
        if (!mTouchDragging) {
            var offsetDelta: Int
            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80
                } else {
                    mFlingVelocity = 0
                }
                mOffset += offsetDelta
                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2
                    mFlingVelocity = 0
                }
                if (mOffset < 0) {
                    mOffset = 0
                    mFlingVelocity = 0
                }
                mOffsetGoal = mOffset
            } else {
                offsetDelta = mOffsetGoal - mOffset
                offsetDelta =
                    if (offsetDelta > 10) offsetDelta / 10 else if (offsetDelta > 0) 1 else if (offsetDelta < -10) offsetDelta / 10 else if (offsetDelta < 0) -1 else 0
                mOffset += offsetDelta
            }
        }
        audioWaveform?.setParameters(mStartPos, mEndPos, mOffset)
        audioWaveform?.invalidate()
        markerStart?.contentDescription = " Start Marker" + formatTime(mStartPos)
        markerEnd?.contentDescription = " End Marker" + formatTime(mEndPos)
        var startX = mStartPos - mOffset - mMarkerLeftInset
        if ((startX + (markerStart?.width ?: 0)) >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                mHandler?.postDelayed({
                    mStartVisible = true
                    markerStart?.alpha = 1f
                    txtStartPosition?.alpha = 1f
                }, 0)
            }
        } else {
            if (mStartVisible) {
                markerStart?.alpha = 0f
                txtStartPosition?.alpha = 0f
                mStartVisible = false
            }
            startX = 0
        }
        var startTextX = mStartPos - mOffset - mTextLeftInset
        if (startTextX + (markerStart?.width ?: 0) < 0) {
            startTextX = 0
        }
        var endX = mEndPos - mOffset - (markerEnd?.width ?: 0) + mMarkerRightInset
        if ((endX + (markerEnd?.width ?: 0)) >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                mHandler?.postDelayed({
                    mEndVisible = true
                    markerEnd?.alpha = 1f
                }, 0)
            }
        } else {
            if (mEndVisible) {
                markerEnd?.alpha = 0f
                mEndVisible = false
            }
            endX = 0
        }
        var endTextX = mEndPos - mOffset - (txtEndPosition?.width ?: 0) + mTextRightInset
        if (endTextX + (markerEnd?.width ?: 0) < 0) {
            endTextX = 0
        }
        audioWaveform?.let { audioWaveform ->
            var params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            markerStart?.let {
                params.setMargins(
                    startX,
                    audioWaveform.measuredHeight / 2 + mMarkerTopOffset,
                    -it.width,
                    -it.height
                )

                it.layoutParams = params
            }
            params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            txtStartPosition?.let {
                params.setMargins(
                    startTextX,
                    mTextTopOffset,
                    -it.width,
                    -it.height
                )

                it.layoutParams = params
            }
            params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            markerEnd?.let {
                params.setMargins(
                    endX,
                    audioWaveform.measuredHeight / 2 + mMarkerBottomOffset,
                    -it.width,
                    -it.height
                )

                it.layoutParams = params
            }
            params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            txtEndPosition?.let {
                params.setMargins(
                    endTextX, audioWaveform.measuredHeight - it.height - mTextBottomOffset,
                    -it.width,
                    -it.height
                )

                it.layoutParams = params
            }
        }

    }

    @Synchronized
    private fun handlePause(onCompletion: Boolean) {
        if (mPlayer != null && mPlayer?.isPlaying() == true) {
            showPlayPause()
            mPlayer?.pause()
        }
        if (onCompletion) {
            mIsMarkerTouch = true
            audioWaveform?.setPlayback(-1)
        }
        mIsPlaying = false
    }

    /**
     * Reset all positions
     */
    private fun resetPositions() {
        mStartPos = audioWaveform?.secondsToPixels(0.0) ?: 0
        mEndPos = audioWaveform?.secondsToPixels((audioWaveform?.maxPos() ?: 0).toDouble()) ?: 0
    }

    private fun formatTime(pixels: Int): String {
        return if (audioWaveform != null && audioWaveform?.isInitialized() == true) {
            formatDecimal(audioWaveform?.pixelsToSeconds(pixels) ?: 0.toDouble())
        } else {
            ""
        }
    }

    private fun formatDecimal(x: Double): String {
        var xWhole = x.toInt()
        var xFrac = (100 * (x - xWhole) + 0.5).toInt()
        if (xFrac >= 100) {
            xWhole++ //Round up
            xFrac -= 100 //Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10 //we need a fraction that is 2 digits long
            }
        }
        return if (xFrac < 10) {
            if (xWhole < 10) "0$xWhole.0$xFrac" else "$xWhole.0$xFrac"
        } else {
            if (xWhole < 10) "0$xWhole.$xFrac" else "$xWhole.$xFrac"
        }
    }

    private fun trap(pos: Int): Int {
        if (pos < 0) return 0
        return if (pos > mMaxPos) mMaxPos else pos
    }

    private fun setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2)
    }

    private fun setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2)
    }

    private fun setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2)
    }

    private fun setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2)
    }

    private fun setOffsetGoal(offset: Int) {
        setOffsetGoalNoUpdate(offset)
        updateDisplay()
    }

    private fun setOffsetGoalNoUpdate(offset: Int) {
        if (mTouchDragging) {
            return
        }
        mOffsetGoal = offset
        if (mOffsetGoal + mWidth / 2 > mMaxPos) mOffsetGoal = mMaxPos - mWidth / 2
        if (mOffsetGoal < 0) mOffsetGoal = 0
    }

    override fun markerTouchStart(marker: MarkerView?, pos: Float) {
        mIsMarkerTouch = true
        audioWaveform?.setPlayback(-1)
        mTouchDragging = true
        mTouchStart = pos
        mTouchInitialStartPos = mStartPos
        mTouchInitialEndPos = mEndPos
        handlePause(false)
    }

    override fun markerTouchMove(marker: MarkerView?, pos: Float) {
        val delta = pos - mTouchStart
        if (marker == markerStart) {
            mStartPos = trap((mTouchInitialStartPos + delta).toInt())
            mEndPos = trap((mTouchInitialEndPos + delta).toInt())
        } else {
            mEndPos = trap((mTouchInitialEndPos + delta).toInt())
            if (mEndPos < mStartPos) mEndPos = mStartPos
        }
        updateDisplay()
    }

    override fun markerTouchEnd(marker: MarkerView?) {
        mIsMarkerTouch = true
        audioWaveform?.setPlayback(-1)
        mTouchDragging = false
        if (marker == markerStart) {
            setOffsetGoalStart()
        } else {
            setOffsetGoalEnd()
        }
    }

    override fun markerFocus(marker: MarkerView?) {
        mKeyDown = false
        if (marker == markerStart) {
            setOffsetGoalStartNoUpdate()
        } else {
            setOffsetGoalEndNoUpdate()
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler?.postDelayed({ updateDisplay() }, 100)
    }

    override fun markerLeft(marker: MarkerView?, velocity: Int) {
        mKeyDown = true
        if (marker == markerStart) {
            val saveStart = mStartPos
            mStartPos = trap(mStartPos - velocity)
            mEndPos = trap(mEndPos - (saveStart - mStartPos))
            setOffsetGoalStart()
        }
        if (marker == markerEnd) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity)
                mEndPos = mStartPos
            } else {
                mEndPos = trap(mEndPos - velocity)
            }
            setOffsetGoalEnd()
        }
        updateDisplay()
    }

    override fun markerRight(marker: MarkerView?, velocity: Int) {
        mKeyDown = true
        if (marker == markerStart) {
            val saveStart = mStartPos
            mStartPos += velocity
            if (mStartPos > mMaxPos) mStartPos = mMaxPos
            mEndPos += mStartPos - saveStart
            if (mEndPos > mMaxPos) mEndPos = mMaxPos
            setOffsetGoalStart()
        }
        if (marker == markerEnd) {
            mEndPos += velocity
            if (mEndPos > mMaxPos) mEndPos = mMaxPos
            setOffsetGoalEnd()
        }
        updateDisplay()
    }

    override fun markerEnter(marker: MarkerView?) {}
    override fun markerKeyUp() {
        mKeyDown = false
        updateDisplay()
    }

    override fun markerDraw() {}
    override fun waveformTouchStart(x: Float) {
        mTouchDragging = true
        mTouchStart = x
        mTouchInitialOffset = mOffset
        mFlingVelocity = 0
    }

    override fun waveformTouchMove(x: Float) {
        mOffset = trap((mTouchInitialOffset + (mTouchStart - x)).toInt())
        updateDisplay()
    }

    override fun waveformTouchEnd() {}
    override fun waveformFling(x: Float) {
        mTouchDragging = false
        mOffsetGoal = mOffset
        mFlingVelocity = (-x).toInt()
        updateDisplay()
    }

    /**
     * Every time we get a message that our waveform drew, see if we need to
     * animate and trigger another redraw.
     */
    override fun waveformDraw() {
        mWidth = audioWaveform?.measuredWidth ?: 0
        if (mOffsetGoal != mOffset && !mKeyDown) updateDisplay() else if (mIsPlaying) {
            updateDisplay()
        } else if (mFlingVelocity != 0) {
            updateDisplay()
        }
    }

    override fun waveformZoomIn() {}
    override fun waveformZoomOut() {}
    private fun saveBtnClick() {
        btnPlay?.isEnabled = false
        btnImgPlay?.isEnabled = false
        val startTime = (audioWaveform?.pixelsToSeconds(mStartPos) ?: 0).toDouble()
        val endTime = (audioWaveform?.pixelsToSeconds(mEndPos) ?: 0).toDouble()
        val difference = endTime - startTime
        val startFrame = audioWaveform?.secondsToFrames(startTime) ?: 0
        val endFrame = audioWaveform?.secondsToFrames(endTime - 0.04) ?: 0
        val duration = (endTime - startTime + 0.5).toInt()
        if (difference <= 0) {
            Toast.makeText(
                this@RecordNewActivity,
                "Trim seconds should be greater than 0 seconds",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Save the sound file in a background thread
        val mSaveSoundFileThread: Thread = object : Thread() {
            override fun run() {
                // Try AAC first.
                val outPath = makeRingtoneFilename("AUDIO_TEMP", AndroidUtils.AUDIO_FORMAT)
                if (outPath == null) {
                    Log.e(" >> ", "Unable to find unique filename")
                    return
                }
                val outFile = File(outPath)
                try {
                    // Write the new file
                    mLoadedSoundFile?.WriteFile(outFile, startFrame, endFrame - startFrame)
                } catch (e: Exception) {
                    // log the error and try to create a .wav file instead
                    if (outFile.exists()) {
                        outFile.delete()
                    }
                    e.printStackTrace()
                }
                val finalOutPath: String = outPath
                val runnable =
                    Runnable { afterSavingRingtone("AUDIO_TEMP", finalOutPath, duration) }
                mHandler?.post(runnable)
            }
        }
        mSaveSoundFileThread.start()
    }

    private fun makeRingtoneFilename(title: CharSequence, extension: String): String? {
        val subDir: String
        var externalRootDir = externalCacheDir?.path
        if (externalRootDir?.endsWith("/")?.not() == true) {
            externalRootDir += "/"
        }
        subDir = "media/audio/music/"
        var parentDir = externalRootDir + subDir

        // Create the parent directory
        val parentDirFile = File(parentDir)
        parentDirFile.mkdirs()

        // If we can't write to that special path, try just writing
        // directly to the sdcard
        if (!parentDirFile.isDirectory) {
            parentDir = externalRootDir.toString()
        }

        // Turn the title into a filename
        var filename = ""
        for (i in 0 until title.length) {
            if (Character.isLetterOrDigit(title[i])) {
                filename += title[i]
            }
        }

        // Try to make the filename unique
        var path: String? = null
        for (i in 0..99) {
            var testPath: String
            testPath =
                if (i > 0) parentDir + filename + i + extension else parentDir + filename + extension
            try {
                val f = RandomAccessFile(File(testPath), "r")
                f.close()
            } catch (e: Exception) {
                // Good, the file didn't exist
                path = testPath
                break
            }
        }
        return path
    }

    /**
     * After saving as ringtone set its content values
     *
     * @param title    title
     * @param outPath  output path
     * @param duration duration of file
     */
    private fun afterSavingRingtone(title: CharSequence, outPath: String, duration: Int) {
        val outFile = File(outPath)
        val fileSize = outFile.length()
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DATA, outPath)
        values.put(MediaStore.MediaColumns.TITLE, title.toString())
        values.put(MediaStore.MediaColumns.SIZE, fileSize)
        values.put(MediaStore.MediaColumns.MIME_TYPE, AndroidUtils.AUDIO_MIME_TYPE)
        values.put(MediaStore.Audio.Media.ARTIST, applicationInfo.name)
        values.put(MediaStore.Audio.Media.DURATION, duration)
        values.put(MediaStore.Audio.Media.IS_MUSIC, true)
        val uri = MediaStore.Audio.Media.getContentUriForPath(outPath)
        val newUri = uri?.let { contentResolver.insert(it, values) }
        Log.e("final URI >> ", newUri.toString() + " >> " + outPath)
        deleteAlreadyRecordAudio = false
        val conData = Bundle()
        conData.putString(Constants.Intents.RETURN_FILENAME, outPath)
        Toast.makeText(this, getString(R.string.txt_recording_save), Toast.LENGTH_LONG).show()
        btnPlay?.isEnabled = true
        btnImgPlay?.isEnabled = true
    }

    @Synchronized
    private fun onPlay(startPosition: Int, mIsMarker: Boolean) {
        if (mIsPlaying) {
            handlePause(false)
            return
        } else {
            showPlayStart(false)
        }
        if (mPlayer == null) {
            // Not initialized yet
            return
        }
        try {
            var mPlayStartMsec = audioWaveform?.pixelsToMillisecs(startPosition) ?: 0
            if (!mIsMarker) {
                mPlayStartMsec = mPlayer?.getCurrentPosition() ?: 0
            }
            mIsMarkerTouch = false
            mPlayEndMillSec = if (startPosition < mStartPos) {
                audioWaveform?.pixelsToMillisecs(mStartPos) ?: 0
            } else if (startPosition > mEndPos) {
                audioWaveform?.pixelsToMillisecs(mMaxPos) ?: 0
            } else {
                audioWaveform?.pixelsToMillisecs(mEndPos) ?: 0
            }
            mPlayer?.setOnCompletionListener(object : SamplePlayer.OnCompletionListener {
                override fun onCompletion() {
                    mIsMarkerTouch = true
                    handlePause(true)
                }
            })
            mIsPlaying = true
            mPlayer?.seekTo(mPlayStartMsec)
            mPlayer?.start()
            updateDisplay()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun askDeleteRecordedAudio() {
        AndroidUtils.showSimpleDialog(
            this@RecordNewActivity,
            R.drawable.ic_delete_forever,
            R.string.warning,
            applicationContext.getString(R.string.delete_this_record),
            { dialog: DialogInterface?, which: Int ->
                deleteRecord = true
                deleteRecordedAudio()
            }
        )
    }

    private fun deleteRecordedAudio() {
        deleteAlreadyRecordAudio = true
        mLoadedSoundFile = null
        if (mPlayer != null && mPlayer?.isPlaying() == true) {
            mIsPlaying = false
            mPlayer?.pause()
        }
        showPlayPause()
        deleteRecord()
    }

    companion object {
        const val REQ_CODE_REC_AUDIO_AND_WRITE_EXTERNAL = 101
        const val REQ_CODE_RECORD_AUDIO = 303
        const val REQ_CODE_WRITE_EXTERNAL_STORAGE = 404
        const val REQ_CODE_READ_EXTERNAL_STORAGE_IMPORT = 405
        const val REQ_CODE_READ_EXTERNAL_STORAGE_PLAYBACK = 406
        const val REQ_CODE_IMPORT_AUDIO = 11
    }

    override fun onMenuItemClick(menuItem: MenuItem?): Boolean {
        return when (menuItem?.itemId) {
            R.id.menuDownload -> {
                // Handle download icon press
                if (mPlayer != null && mPlayer?.isPlaying() == true) {
                    showPlayPause()
                    mIsPlaying = false
                    mPlayer?.pause()
                }
                saveBtnClick()
                true
            }
            R.id.menuRestore -> {

                if (mPlayer != null && mPlayer?.isPlaying() == true) {
                    mIsPlaying = false
                    mPlayer?.pause()
                }
                presenter?.pausePlayback()
                deleteRecord = true
                presenter?.stopRecording(true)
                initTrim()
                initVisibleBtn()
                initWave()
                true
            }
            else -> false
        }
    }
}