package com.wave.audiorecording.player

import com.wave.audiorecording.exception.AppException

interface PlayerContract {
    interface PlayerCallback {
        fun onPreparePlay()
        fun onStartPlay()
        fun onPlayProgress(mills: Long)
        fun onStopPlay()
        fun onPausePlay()
        fun onSeek(mills: Long)
        fun onError(throwable: AppException?)
    }

    interface Player {
        fun addPlayerCallback(callback: PlayerCallback?)
        fun removePlayerCallback(callback: PlayerCallback?): Boolean
        fun setData(data: String)
        fun playOrPause()
        fun seek(mills: Long)
        fun pause()
        fun stop()
        val isPlaying: Boolean
        val isPause: Boolean
        val pauseTime: Long
        fun release()
    }
}