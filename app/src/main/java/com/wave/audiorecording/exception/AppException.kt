package com.wave.audiorecording.exception

abstract class AppException : Exception() {
    abstract val type: Int

    companion object {
        const val CANT_CREATE_FILE = 1
        const val INVALID_OUTPUT_FILE = 2
        const val RECORDER_INIT_EXCEPTION = 3
        const val PLAYER_INIT_EXCEPTION = 4
        const val PLAYER_DATA_SOURCE_EXCEPTION = 5
        const val CANT_PROCESS_RECORD = 6
        const val READ_PERMISSION_DENIED = 7
        const val NO_SPACE_AVAILABLE = 8
        const val RECORDING_ERROR = 9
    }
}