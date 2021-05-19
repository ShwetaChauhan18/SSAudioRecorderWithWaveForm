package com.wave.audiorecording.app

import android.util.Log
import com.wave.audiorecording.BackgroundQueue
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.audioplayer.AudioDecoder.Companion.decode
import com.wave.audiorecording.audioplayer.AudioDecoder.DecodeListener
import com.wave.audiorecording.audioplayer.recorder.RecorderContract
import com.wave.audiorecording.audioplayer.recorder.RecorderContract.RecorderCallback
import com.wave.audiorecording.data.Prefs
import com.wave.audiorecording.data.database.LocalRepository
import com.wave.audiorecording.data.database.Record
import com.wave.audiorecording.exception.AppException
import com.wave.audiorecording.util.AndroidUtils
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.IntArrayList
import java.io.File
import java.util.ArrayList

class AppRecorderImpl private constructor(private var audioRecorder: RecorderContract.Recorder,
                                          private val localRepository: LocalRepository, private val recordingsTasks: BackgroundQueue,
                                          private val processingTasks: BackgroundQueue, private val prefs: Prefs) : AppRecorder {
    private val recorderCallback: RecorderCallback
    private val appCallbacks: MutableList<AppRecorderCallback?>
    override val recordingData: IntArrayList
    override var recordingDuration: Long = 0
    override var isProcessing = false
        private set

    private fun convertRecordingData(list: IntArrayList, durationSec: Int): IntArray {
        return if (durationSec > AppConstants.LONG_RECORD_THRESHOLD_SECONDS) {
            val sampleCount = AudioRecordingWavesApplication.longWaveformSampleCount
            val waveForm = IntArray(sampleCount)
            if (list.size() < sampleCount * 2) {
                val scale = list.size().toFloat() / sampleCount.toFloat()
                for (i in 0 until sampleCount) {
                    waveForm[i] = convertAmp(list[Math.floor((i * scale).toDouble()).toInt()].toDouble())
                }
            } else {
                val scale = list.size().toFloat() / sampleCount.toFloat()
                for (i in 0 until sampleCount) {
                    var value = 0
                    val step = Math.ceil(scale.toDouble()).toInt()
                    for (j in 0 until step) {
                        value += list[(i * scale + j).toInt()]
                    }
                    value = (value.toFloat() / scale).toInt()
                    waveForm[i] = convertAmp(value.toDouble())
                }
            }
            waveForm
        } else {
            val waveForm = IntArray(list.size())
            for (i in 0 until list.size()) {
                waveForm[i] = convertAmp(list[i].toDouble())
            }
            waveForm
        }
    }

    /**
     * Convert dB amp value to view amp.
     */
    private fun convertAmp(amp: Double): Int {
        return (255 * (amp / 32767f)).toInt()
    }

    override fun decodeRecordWaveform(decRec: Record) {
        processingTasks.postRunnable({
            isProcessing = true
            val path = decRec.path
            if (path != null && !path.isEmpty()) {
                decode(path, object : DecodeListener {
                    override fun onStartDecode(duration: Long, channelsCount: Int, sampleRate: Int) {
                        decRec.duration = duration
                        AndroidUtils.runOnUIThread( { onRecordProcessing() })
                    }

                    override fun onFinishDecode(data: IntArray?, duration: Long) {
                        val rec = data?.let {
                            Record(
                                    decRec.id,
                                    decRec.getName(),
                                    decRec.duration,
                                    decRec.created,
                                    decRec.added,
                                    decRec.removed,
                                    decRec.path,
                                    decRec.isBookmarked,
                                    true,
                                    it)
                        }
                        rec?.let { localRepository.updateRecord(it) }
                        isProcessing = false
                        AndroidUtils.runOnUIThread( { onRecordFinishProcessing() })
                    }

                    override fun onError(exception: Exception?) {
                        isProcessing = false
                    }
                })
            } else {
                isProcessing = false
                Log.e("AppRecorderImpl", "File path is null or empty")
            }
        })
    }

    override fun addRecordingCallback(callback: AppRecorderCallback?) {
        appCallbacks.add(callback)
    }

    override fun removeRecordingCallback(callback: AppRecorderCallback?) {
        appCallbacks.remove(callback)
    }

    override fun setRecorder(recorder: RecorderContract.Recorder) {
        audioRecorder = recorder
        audioRecorder.setRecorderCallback(recorderCallback)
    }

    override fun startRecording(filePath: String?, channelCount: Int, sampleRate: Int, bitrate: Int) {
        if (!audioRecorder.isRecording) {
            audioRecorder.prepare(filePath, channelCount, sampleRate, bitrate)
        }
    }

    override fun pauseRecording() {
        if (audioRecorder.isRecording) {
            audioRecorder.pauseRecording()
        }
    }

    override fun resumeRecording() {
        if (audioRecorder.isPaused) {
            audioRecorder.startRecording()
        }
    }

    override fun stopRecording() {
        if (audioRecorder.isRecording) {
            audioRecorder.stopRecording()
        }
    }

    override val isRecording: Boolean
        get() = audioRecorder.isRecording
    override val isPaused: Boolean
        get() = audioRecorder.isPaused

    override fun release() {
        recordingData.clear()
        audioRecorder.stopRecording()
        appCallbacks.clear()
    }

    private fun onRecordingStarted(output: File?) {
        if (!appCallbacks.isEmpty()) {
            for (i in appCallbacks.indices) {
                appCallbacks[i]?.onRecordingStarted(output)
            }
        }
    }

    private fun onRecordingPaused() {
        if (!appCallbacks.isEmpty()) {
            for (i in appCallbacks.indices) {
                appCallbacks[i]?.onRecordingPaused()
            }
        }
    }

    private fun onRecordProcessing() {
        isProcessing = true
        if (!appCallbacks.isEmpty()) {
            for (i in appCallbacks.indices) {
                appCallbacks[i]?.onRecordProcessing()
            }
        }
    }

    private fun onRecordFinishProcessing() {
        isProcessing = false
        if (!appCallbacks.isEmpty()) {
            for (i in appCallbacks.indices) {
                appCallbacks[i]?.onRecordFinishProcessing()
            }
        }
    }

    private fun onRecordingStopped(file: File?, record: Record) {
        if (!appCallbacks.isEmpty()) {
            for (i in appCallbacks.indices) {
                appCallbacks[i]?.onRecordingStopped(file, record)
            }
        }
    }

    private fun onRecordingProgress(mills: Long, amp: Int) {
        if (!appCallbacks.isEmpty()) {
            for (i in appCallbacks.indices) {
                appCallbacks[i]?.onRecordingProgress(mills, amp)
            }
        }
    }

    private fun onRecordingError(e: AppException?) {
        if (!appCallbacks.isEmpty()) {
            for (i in appCallbacks.indices) {
                appCallbacks[i]?.onError(e)
            }
        }
    }

    companion object {
        @Volatile
        private lateinit var instance: AppRecorderImpl

        @JvmStatic
        fun getInstance(recorder: RecorderContract.Recorder, localRep: LocalRepository, tasks: BackgroundQueue, processingTasks: BackgroundQueue, prefs: Prefs): AppRecorderImpl {
            if (!::instance.isInitialized) {
                synchronized(AppRecorderImpl::class.java) {
                    if (!::instance.isInitialized) {
                        instance = AppRecorderImpl(recorder, localRep, tasks, processingTasks, prefs)
                    }
                }
            }
            return instance
        }
    }

    init {
        appCallbacks = ArrayList()
        recordingData = IntArrayList()
        recorderCallback = object : RecorderCallback {
            override fun onPrepareRecord() {
                audioRecorder.startRecording()
            }

            override fun onStartRecord(output: File?) {
                recordingDuration = 0
                onRecordingStarted(output)
            }

            override fun onPauseRecord() {
                onRecordingPaused()
            }

            override fun onRecordProgress(mills: Long, amplitude: Int) {
                recordingDuration = mills
                onRecordingProgress(mills, amplitude)
                recordingData.add(amplitude)
            }

            override fun onStopRecord(output: File?) {
                recordingDuration = 0
                recordingsTasks.postRunnable({
                    val duration = output?.let { AndroidUtils.readRecordDuration(it) } ?: 0
                    val waveForm = convertRecordingData(recordingData, (duration / 1000000f).toInt())
                    val record = localRepository.getRecord(prefs.activeRecord.toInt())
                    if (record != null) {
                        val update = Record(
                                record.id,
                                record.getName(),
                                duration,
                                record.created,
                                record.added,
                                record.removed,
                                record.path,
                                record.isBookmarked,
                                record.isWaveformProcessed,
                                waveForm)
                        if (localRepository.updateRecord(update)) {
                            recordingData.clear()
                            val rec = localRepository.getRecord(update.id)
                            rec?.let {
                                AndroidUtils.runOnUIThread( { onRecordingStopped(output, it) })
                                decodeRecordWaveform(it)
                            }
                        } else {
                            //Try to update record again if failed.
                            if (localRepository.updateRecord(update)) {
                                recordingData.clear()
                                val rec = localRepository.getRecord(update.id)
                                rec?.let {
                                    AndroidUtils.runOnUIThread( { onRecordingStopped(output, it) })
                                    decodeRecordWaveform(it)
                                }
                            } else {
                                onRecordingStopped(output, record)
                            }
                        }
                    } else {
                        //TODO: Error on record update.
                    }
                })
            }

            override fun onError(e: AppException?) {
                onRecordingError(e)
            }
        }
        audioRecorder.setRecorderCallback(recorderCallback)
    }
}