package com.wave.audiorecording.audioplayer.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.audioplayer.recorder.RecorderContract.RecorderCallback
import com.wave.audiorecording.exception.InvalidOutputFile
import com.wave.audiorecording.exception.RecorderInitException
import com.wave.audiorecording.exception.RecordingException
import com.wave.audiorecording.util.AndroidUtils
import com.wave.audiorecording.util.AppConstants
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Timer
import java.util.TimerTask

class WavRecorder private constructor() : RecorderContract.Recorder {
    private var recorder: AudioRecord? = null
    private var recordFile: File? = null
    private var bufferSize = 0
    private var recordingThread: Thread? = null
    override var isRecording = false
        private set
    override var isPaused = false
        private set
    private var channelCount = 1

    /**
     * Value for recording used visualisation.
     */
    private var lastVal = 0
    private var timerProgress: Timer? = null
    private var progress: Long = 0
    private var sampleRate = AppConstants.RECORD_SAMPLE_RATE_44100
    private var recorderCallback: RecorderCallback? = null
    override fun setRecorderCallback(callback: RecorderCallback?) {
        recorderCallback = callback
    }

    override fun prepare(outputFile: String?, channelCount: Int, sampleRate: Int, bitrate: Int) {
        this.sampleRate = sampleRate
        //		this.framesPerVisInterval = (int)((VISUALIZATION_INTERVAL/1000f)/(1f/sampleRate));
        this.channelCount = channelCount
        recordFile = File(outputFile)
        if (recordFile?.exists() == true && recordFile?.isFile == true) {
            val channel =
                if (channelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
            try {
                bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT
                )
                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    bufferSize = AudioRecord.getMinBufferSize(
                        sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT
                    )
                }
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channel,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            } catch (e: IllegalArgumentException) {
                Log.e(
                    "WavRecorder",
                    "sampleRate = $sampleRate channel = $channel bufferSize = $bufferSize"
                )
                if (recorder != null) {
                    recorder?.release()
                }
            }
            if (recorder != null && recorder?.state == AudioRecord.STATE_INITIALIZED) {
                if (recorderCallback != null) {
                    recorderCallback?.onPrepareRecord()
                }
            } else {
                Log.e("WavRecorder", "prepare() failed")
                if (recorderCallback != null) {
                    recorderCallback?.onError(RecorderInitException())
                }
            }
        } else {
            if (recorderCallback != null) {
                recorderCallback?.onError(InvalidOutputFile())
            }
        }
    }

    override fun startRecording() {
        if (recorder != null && recorder?.state == AudioRecord.STATE_INITIALIZED) {
            if (isPaused) {
                startRecordingTimer()
                recorder?.startRecording()
                if (recorderCallback != null) {
                    recorderCallback?.onStartRecord(recordFile)
                }
                isPaused = false
            } else {
                try {
                    recorder?.startRecording()
                    isRecording = true
                    recordingThread = Thread({ writeAudioDataToFile() }, "AudioRecorder Thread")
                    recordingThread?.start()
                    startRecordingTimer()
                    if (recorderCallback != null) {
                        recorderCallback?.onStartRecord(recordFile)
                    }
                    AudioRecordingWavesApplication.isRecording = (true)
                } catch (e: IllegalStateException) {
                    Log.e("WavRecorder", "startRecording() failed")
                    if (recorderCallback != null) {
                        recorderCallback?.onError(RecorderInitException())
                    }
                }
            }
        }
    }

    override fun pauseRecording() {
        if (isRecording) {
            recorder?.stop()
            pauseRecordingTimer()
            isPaused = true
            if (recorderCallback != null) {
                recorderCallback?.onPauseRecord()
            }
        }
    }

    override fun stopRecording() {
        if (recorder != null) {
            isRecording = false
            isPaused = false
            stopRecordingTimer()
            if (recorder?.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    recorder?.stop()
                    AudioRecordingWavesApplication.isRecording = false
                } catch (e: IllegalStateException) {
                    Log.e("WavRecorder", "stopRecording() problems")
                }
            }
            recorder?.release()
            recordingThread?.interrupt()
            if (recorderCallback != null) {
                recorderCallback?.onStopRecord(recordFile)
            }
        }
    }

    private fun writeAudioDataToFile() {
        val data = ByteArray(bufferSize)
        val fos: FileOutputStream? = try {
            FileOutputStream(recordFile)
        } catch (e: FileNotFoundException) {
            Log.e("WavRecorder", e.message.toString())
            null
        }
        if (null != fos) {
            var chunksCount = 0
            val shortBuffer = ByteBuffer.allocate(2)
            shortBuffer.order(ByteOrder.LITTLE_ENDIAN)
            //TODO: Disable loop while pause.
            recorder?.let {
                while (isRecording) {
                    if (!isPaused) {
                        chunksCount += it.read(data, 0, bufferSize)
                        if (AudioRecord.ERROR_INVALID_OPERATION != chunksCount) {
                            var sum: Long = 0
                            var i = 0
                            while (i < bufferSize) {

                                //TODO: find a better way to covert bytes into shorts.
                                shortBuffer.put(data[i])
                                shortBuffer.put(data[i + 1])
                                sum += Math.abs(shortBuffer.getShort(0).toInt()).toLong()
                                shortBuffer.clear()
                                i += 2
                            }
                            lastVal = (sum / (bufferSize / 16)).toInt()
                            try {
                                fos.write(data)
                            } catch (e: IOException) {
                                Log.e("WavRecorder", e.message.toString())
                                AndroidUtils.runOnUIThread({
                                    recorderCallback?.onError(RecordingException())
                                    stopRecording()
                                })
                            }
                        }
                    }
                }
            }
            try {
                fos.close()
            } catch (e: IOException) {
                Log.e("WavRecorder", e.message.toString())
            }
            setWaveFileHeader(recordFile, channelCount)
        }
    }

    private fun setWaveFileHeader(file: File?, channels: Int) {
        val fileSize = (file?.length() ?: 0) - 8
        val totalSize = fileSize + 36
        val byteRate =
            (sampleRate * channels * (RECORDER_BPP / 8)).toLong() //2 byte per 1 sample for 1 channel.
        try {
            val wavFile = randomAccessFile(file)
            wavFile.seek(0) // to the beginning
            wavFile.write(
                generateHeader(
                    fileSize,
                    totalSize,
                    sampleRate.toLong(),
                    channels,
                    byteRate
                )
            )
            wavFile.close()
        } catch (e: FileNotFoundException) {
            Log.e("WavRecorder", e.message.toString())
        } catch (e: IOException) {
            Log.e("WavRecorder", e.message.toString())
        }
    }

    private fun randomAccessFile(file: File?): RandomAccessFile {
        val randomAccessFile: RandomAccessFile
        randomAccessFile = try {
            RandomAccessFile(file, "rw")
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }
        return randomAccessFile
    }

    private fun generateHeader(
        totalAudioLen: Long, totalDataLen: Long, longSampleRate: Long, channels: Int, byteRate: Long
    ): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF/WAVE header
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 //16 for PCM. 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * (RECORDER_BPP / 8)).toByte() // block align
        header[33] = 0
        header[34] = RECORDER_BPP.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        return header
    }

    private fun startRecordingTimer() {
        timerProgress = Timer()
        timerProgress?.schedule(object : TimerTask() {
            override fun run() {
                if (recorderCallback != null && recorder != null) {
                    recorderCallback?.onRecordProgress(progress, lastVal)
                    progress += AppConstants.VISUALIZATION_INTERVAL
                }
            }
        }, 0, AppConstants.VISUALIZATION_INTERVAL)
    }

    private fun stopRecordingTimer() {
        timerProgress?.cancel()
        timerProgress?.purge()
        progress = 0
    }

    private fun pauseRecordingTimer() {
        timerProgress?.cancel()
        timerProgress?.purge()
    }

    private object WavRecorderSingletonHolder {
        val singleton = WavRecorder()
    }

    companion object {
        private const val RECORDER_BPP = 16 //bits per sample

        @JvmStatic
        val instance: WavRecorder
            get() = WavRecorderSingletonHolder.singleton
    }
}