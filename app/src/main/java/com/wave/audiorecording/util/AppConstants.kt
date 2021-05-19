package com.wave.audiorecording.util

/**
 * AppConstants that may be used in multiple classes.
 */
object AppConstants {
    const val APPLICATION_NAME = "AudioWavesRecording"
    const val RECORDS_DIR = "records"
    const val M4A_EXTENSION = "m4a"
    const val WAV_EXTENSION = "wav"
    const val EXTENSION_SEPARATOR = "."
    const val BASE_RECORD_NAME = "Record-"
    const val BASE_RECORD_NAME_SHORT = "Rec-"
    const val TRASH_MARK_EXTENSION = "del"
    const val MAX_RECORD_NAME_LENGTH = 50
    const val NAMING_COUNTED = 0
    const val NAMING_DATE = 1
    const val RECORDING_FORMAT_M4A = 0
    const val RECORDING_FORMAT_WAV = 1
    const val DEFAULT_PER_PAGE = 50
    const val RECORD_IN_TRASH_MAX_DURATION = 5184000000L // 1000 X 60 X 60 X 24 X 60 = 60 Days
    const val MIN_REMAIN_RECORDING_TIME: Long = 10000 // 1000 X 60 = 1 Minute

    /**
     * Density pixel count per one second of time.
     * Used for short records (shorter than [AppConstants.LONG_RECORD_THRESHOLD_SECONDS])
     */
    const val SHORT_RECORD_DP_PER_SECOND = 25L
    //BEGINNING-------------- Waveform visualisation constants ----------------------------------
    /**
     * Waveform length, measured in screens count of device.
     * Used for long records (longer than [AppConstants.LONG_RECORD_THRESHOLD_SECONDS])
     */
    const val WAVEFORM_WIDTH = 1.5f //one and half of screen waveform width.

    /**
     * Threshold in second which defines when record is considered as long or short.
     * For short and long records used a bit different visualisation algorithm.
     */
    const val LONG_RECORD_THRESHOLD_SECONDS = 20

    /**
     * Count of grid lines on visible part of Waveform (actually lines count visible on screen).
     * Used for long records visualisation algorithm. (longer than [AppConstants.LONG_RECORD_THRESHOLD_SECONDS] )
     */
    const val GRID_LINES_COUNT = 16
    const val TIME_FORMAT_24H = 11

    //END-------------- Waveform visualisation constants ----------------------------------------
    const val TIME_FORMAT_12H = 12

    // recording and playback
    const val PLAYBACK_SAMPLE_RATE = 44100
    const val RECORD_SAMPLE_RATE_44100 = 44100
    const val RECORD_SAMPLE_RATE_8000 = 8000
    const val RECORD_SAMPLE_RATE_16000 = 16000
    const val RECORD_SAMPLE_RATE_32000 = 32000
    const val RECORD_SAMPLE_RATE_48000 = 48000
    const val RECORD_ENCODING_BITRATE_24000 = 24000
    const val RECORD_ENCODING_BITRATE_48000 = 48000
    const val RECORD_ENCODING_BITRATE_96000 = 96000
    const val RECORD_ENCODING_BITRATE_128000 = 128000
    const val RECORD_ENCODING_BITRATE_192000 = 192000
    const val SORT_DATE = 1
    const val SORT_NAME = 2
    const val SORT_DURATION = 3
    const val SORT_DATE_DESC = 4
    const val SORT_NAME_DESC = 5
    const val SORT_DURATION_DESC = 6
    const val RECORD_AUDIO_MONO = 1
    const val RECORD_AUDIO_STEREO = 2

    /**
     * Time interval for Recording progress visualisation.
     */
    const val VISUALIZATION_INTERVAL = 1000 / SHORT_RECORD_DP_PER_SECOND //1000 mills/25 dp per sec
}