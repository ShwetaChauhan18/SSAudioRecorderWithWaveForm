package com.wave.audiorecording.audioplayer

import android.media.MediaCodec
import android.media.MediaCodec.CodecException
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.util.AppConstants
import com.wave.audiorecording.util.IntArrayList
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays

/**
 * AudioDecoder
 */
class AudioDecoder private constructor() {
    private var dpPerSec = AppConstants.SHORT_RECORD_DP_PER_SECOND.toFloat()
    private var sampleRate = 0
    private var channelCount = 0
    private lateinit var oneFrameAmps: IntArray
    private var frameIndex = 0
    private var duration: Long = 0
    private var gains: IntArrayList? = null
    private fun calculateSamplesPerFrame(): Int {
        return (sampleRate / dpPerSec).toInt()
    }

    @Throws(IOException::class, OutOfMemoryError::class, IllegalStateException::class)
    private fun decodeFile(mInputFile: File, decodeListener: DecodeListener, queueType: Int) {
        gains = IntArrayList()
        val extractor = MediaExtractor()
        var format: MediaFormat? = null
        var i: Int
        extractor.setDataSource(mInputFile.path)
        val numTracks = extractor.trackCount
        // find and select the first audio track present in the file.
        i = 0
        while (i < numTracks) {
            format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                extractor.selectTrack(i)
                break
            }
            i++
        }
        if (i == numTracks || format == null) {
            throw IOException("No audio track found in $mInputFile")
        }
        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        duration = format.getLong(MediaFormat.KEY_DURATION)

        //TODO: Make waveform independent from dpPerSec?!
        dpPerSec = AudioRecordingWavesApplication.getDpPerSecond(duration.toFloat() / 1000000f)
        oneFrameAmps = IntArray(calculateSamplesPerFrame() * channelCount)
        val mimeType = format.getString(MediaFormat.KEY_MIME)
        //Start decoding
        val decoder = mimeType?.let { MediaCodec.createDecoderByType(it) }
        decodeListener.onStartDecode(duration, channelCount, sampleRate)
        decoder?.setCallback(object : MediaCodec.Callback() {
            private var mOutputEOS = false
            private var mInputEOS = false
            override fun onError(codec: MediaCodec, exception: CodecException) {
                Log.e("AudioDeocder", exception.message.toString())
                if (queueType == QUEUE_INPUT_BUFFER_EFFECTIVE) {
                    try {
                        val decoder = AudioDecoder()
                        decoder.decodeFile(mInputFile, decodeListener, QUEUE_INPUT_BUFFER_SIMPLE)
                    } catch (e: IllegalStateException) {
                        decodeListener.onError(exception)
                    } catch (e: IOException) {
                        decodeListener.onError(exception)
                    } catch (e: OutOfMemoryError) {
                        decodeListener.onError(exception)
                    }
                } else {
                    decodeListener.onError(exception)
                }
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                if (mOutputEOS or mInputEOS) return
                try {
                    val inputBuffer: ByteBuffer?
                    inputBuffer = codec.getInputBuffer(index)
                    if (inputBuffer == null) return
                    var sampleTime: Long = 0
                    var result: Int
                    if (queueType == QUEUE_INPUT_BUFFER_EFFECTIVE) {
                        var total = 0
                        var advanced = false
                        var maxresult = 0
                        do {
                            result = extractor.readSampleData(inputBuffer, total)
                            if (result >= 0) {
                                total += result
                                sampleTime = extractor.sampleTime
                                advanced = extractor.advance()
                                maxresult = Math.max(maxresult, result)
                            }
                        } while (result >= 0 && total < maxresult * 5 && advanced && inputBuffer.capacity() - inputBuffer.limit() > maxresult * 3) //3 it is just for insurance. When remove it crash happens. it is ok if replace it by 2 number.
                        if (advanced) {
                            codec.queueInputBuffer(index, 0, total, sampleTime, 0)
                        } else {
                            codec.queueInputBuffer(index, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            mInputEOS = true
                        }
                    } else {
                        //If QUEUE_INPUT_BUFFER_EFFECTIVE failed then trying this way.
                        result = extractor.readSampleData(inputBuffer, 0)
                        if (result >= 0) {
                            sampleTime = extractor.sampleTime
                            codec.queueInputBuffer(index, 0, result, sampleTime, 0)
                            extractor.advance()
                        } else {
                            codec.queueInputBuffer(index, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            mInputEOS = true
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e("AudioDecoder", e.message.toString())
                } catch (e: IllegalArgumentException) {
                    Log.e("AudioDecoder", e.message.toString())
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                val outputBuffer = codec.getOutputBuffer(index)
                if (outputBuffer != null) {
                    outputBuffer.rewind()
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    while (outputBuffer.remaining() > 0) {
                        oneFrameAmps[frameIndex] = outputBuffer.short.toInt()
                        frameIndex++
                        if (frameIndex >= oneFrameAmps.size - 1) {
                            var j: Int
                            var gain: Int
                            var value: Int
                            gain = -1
                            j = 0
                            while (j < oneFrameAmps.size) {
                                value = 0
                                for (k in 0 until channelCount) {
                                    value += oneFrameAmps[j + k]
                                }
                                value /= channelCount
                                if (gain < value) {
                                    gain = value
                                }
                                j += channelCount
                            }
                            gains?.add(Math.sqrt(gain.toDouble()).toInt())
                            frameIndex = 0
                        }
                    }
                }
                mOutputEOS = mOutputEOS or (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                codec.releaseOutputBuffer(index, false)
                if (mOutputEOS) {
                    decodeListener.onFinishDecode(gains?.getData(), duration)
                    codec.stop()
                    codec.release()
                    extractor.release()
                }
            }
        })
        decoder?.configure(format, null, null, 0)
        decoder?.start()
    }

    interface DecodeListener {
        fun onStartDecode(duration: Long, channelsCount: Int, sampleRate: Int)
        fun onFinishDecode(data: IntArray?, duration: Long)
        fun onError(exception: Exception?)
    }

    companion object {
        private const val QUEUE_INPUT_BUFFER_EFFECTIVE = 1 // Most effective and fastest
        private const val QUEUE_INPUT_BUFFER_SIMPLE = 2 // Less effective and slower
        private val SUPPORTED_EXT = arrayOf("mp3", "wav", "3gpp", "3gp", "amr", "aac", "m4a", "mp4", "ogg", "flac")
        @JvmStatic
        fun decode(fileName: String, decodeListener: DecodeListener) {
            try {
                val file = File(fileName)
                if (!file.exists()) {
                    throw FileNotFoundException(fileName)
                }
                val name = file.name.toLowerCase()
                val components = name.split("\\.".toRegex()).toTypedArray()
                if (components.size < 2) {
                    throw IOException()
                }
                if (!Arrays.asList(*SUPPORTED_EXT).contains(components[components.size - 1])) {
                    throw IOException()
                }
                val decoder = AudioDecoder()
                decoder.decodeFile(file, decodeListener, QUEUE_INPUT_BUFFER_EFFECTIVE)
            } catch (e: Exception) {
                decodeListener.onError(e)
            }
        }
    }
}