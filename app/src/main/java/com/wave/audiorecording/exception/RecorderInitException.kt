package com.wave.audiorecording.exception

class RecorderInitException : AppException() {
    override val type: Int
        get() = RECORDER_INIT_EXCEPTION
}