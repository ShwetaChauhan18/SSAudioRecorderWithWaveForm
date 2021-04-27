package com.wave.audiorecording.exception

class RecordingException : AppException() {
    override val type: Int
        get() = RECORDING_ERROR
}