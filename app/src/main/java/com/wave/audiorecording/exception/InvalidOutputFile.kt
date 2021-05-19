package com.wave.audiorecording.exception

class InvalidOutputFile : AppException() {
    override val type: Int
        get() = INVALID_OUTPUT_FILE
}