package com.wave.audiorecording.data

import android.content.Context
import android.content.SharedPreferences
import com.wave.audiorecording.util.AppConstants

/**
 * App preferences implementation
 */
class PrefsImpl private constructor(context: Context) : Prefs {
    private val sharedPreferences: SharedPreferences
    override val isFirstRun: Boolean
        get() = !sharedPreferences.contains(PREF_KEY_IS_FIRST_RUN) || sharedPreferences.getBoolean(PREF_KEY_IS_FIRST_RUN, false)

    override fun firstRunExecuted() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(PREF_KEY_IS_FIRST_RUN, false)
        editor.putBoolean(PREF_KEY_IS_STORE_DIR_PUBLIC, true)
        editor.apply()
        //		setStoreDirPublic(true);
    }

    override var isStoreDirPublic: Boolean
        get() = sharedPreferences.contains(PREF_KEY_IS_STORE_DIR_PUBLIC) && sharedPreferences.getBoolean(PREF_KEY_IS_STORE_DIR_PUBLIC, true)
        set(b) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(PREF_KEY_IS_STORE_DIR_PUBLIC, b)
            editor.apply()
        }
    override var isAskToRenameAfterStopRecording: Boolean
        get() = sharedPreferences.contains(PREF_KEY_IS_ASK_TO_RENAME_AFTER_STOP_RECORDING) && sharedPreferences.getBoolean(PREF_KEY_IS_ASK_TO_RENAME_AFTER_STOP_RECORDING, true)
        set(b) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(PREF_KEY_IS_ASK_TO_RENAME_AFTER_STOP_RECORDING, b)
            editor.apply()
        }

    override fun hasAskToRenameAfterStopRecordingSetting(): Boolean {
        return sharedPreferences.contains(PREF_KEY_IS_ASK_TO_RENAME_AFTER_STOP_RECORDING)
    }

    override var activeRecord: Long
        get() = sharedPreferences.getLong(PREF_KEY_ACTIVE_RECORD, -1)
        set(id) {
            val editor = sharedPreferences.edit()
            editor.putLong(PREF_KEY_ACTIVE_RECORD, id)
            editor.apply()
        }
    override val recordCounter: Long
        get() = sharedPreferences.getLong(PREF_KEY_RECORD_COUNTER, 0)

    override fun incrementRecordCounter() {
        val editor = sharedPreferences.edit()
        editor.putLong(PREF_KEY_RECORD_COUNTER, recordCounter + 1)
        editor.apply()
    }

    override fun setRecordInStereo(stereo: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putInt(PREF_KEY_RECORD_CHANNEL_COUNT, if (stereo) AppConstants.RECORD_AUDIO_STEREO else AppConstants.RECORD_AUDIO_MONO)
        editor.apply()
    }

    override val recordChannelCount: Int
        get() = sharedPreferences.getInt(PREF_KEY_RECORD_CHANNEL_COUNT, AppConstants.RECORD_AUDIO_STEREO)
    override var isKeepScreenOn: Boolean
        get() = sharedPreferences.getBoolean(PREF_KEY_KEEP_SCREEN_ON, false)
        set(on) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(PREF_KEY_KEEP_SCREEN_ON, on)
            editor.apply()
        }
    override var format: Int
        get() = sharedPreferences.getInt(PREF_KEY_FORMAT, AppConstants.RECORDING_FORMAT_M4A)
        set(f) {
            val editor = sharedPreferences.edit()
            editor.putInt(PREF_KEY_FORMAT, f)
            editor.apply()
        }
    override var bitrate: Int
        get() = sharedPreferences.getInt(PREF_KEY_BITRATE, AppConstants.RECORD_ENCODING_BITRATE_128000)
        set(q) {
            val editor = sharedPreferences.edit()
            editor.putInt(PREF_KEY_BITRATE, q)
            editor.apply()
        }
    override var sampleRate: Int
        get() = sharedPreferences.getInt(PREF_KEY_SAMPLE_RATE, AppConstants.RECORD_SAMPLE_RATE_44100)
        set(rate) {
            val editor = sharedPreferences.edit()
            editor.putInt(PREF_KEY_SAMPLE_RATE, rate)
            editor.apply()
        }

    override fun setRecordOrder(order: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(PREF_KEY_RECORDS_ORDER, order)
        editor.apply()
    }

    override val recordsOrder: Int
        get() = sharedPreferences.getInt(PREF_KEY_RECORDS_ORDER, AppConstants.SORT_DATE)
    override var namingFormat: Int
        get() = sharedPreferences.getInt(PREF_KEY_NAMING_FORMAT, AppConstants.NAMING_COUNTED)
        set(format) {
            val editor = sharedPreferences.edit()
            editor.putInt(PREF_KEY_NAMING_FORMAT, format)
            editor.apply()
        }

    companion object {
        private const val PREF_NAME = "com.wave.audiorecording.data.PrefsImpl"
        private const val PREF_KEY_IS_FIRST_RUN = "is_first_run"
        private const val PREF_KEY_IS_STORE_DIR_PUBLIC = "is_store_dir_public"
        private const val PREF_KEY_IS_ASK_TO_RENAME_AFTER_STOP_RECORDING = "is_ask_rename_after_stop_recording"
        private const val PREF_KEY_ACTIVE_RECORD = "active_record"
        private const val PREF_KEY_RECORD_COUNTER = "record_counter"
        private const val PREF_KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val PREF_KEY_FORMAT = "pref_format"
        private const val PREF_KEY_BITRATE = "pref_bitrate"
        private const val PREF_KEY_SAMPLE_RATE = "pref_sample_rate"
        private const val PREF_KEY_RECORDS_ORDER = "pref_records_order"
        private const val PREF_KEY_NAMING_FORMAT = "pref_naming_format"

        //Recording prefs.
        private const val PREF_KEY_RECORD_CHANNEL_COUNT = "record_channel_count"

        @Volatile
        private lateinit var instance: PrefsImpl
        @JvmStatic
        fun getInstance(context: Context): PrefsImpl {
            if (::instance.isInitialized.not()) {
                synchronized(PrefsImpl::class.java) {
                    if (::instance.isInitialized.not()) {
                        instance = PrefsImpl(context)
                    }
                }
            }
            return instance
        }
    }

    init {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}