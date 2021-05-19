package com.wave.audiorecording.exception

class CantCreateFileException : AppException() {
    override val type: Int
        get() = CANT_CREATE_FILE
}