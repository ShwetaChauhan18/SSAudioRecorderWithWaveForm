package com.wave.audiorecording.exception

class PlayerDataSourceException : AppException() {
    override val type: Int
        get() = PLAYER_DATA_SOURCE_EXCEPTION
}