package com.wave.audiorecording.player

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.util.Log
import com.wave.audiorecording.exception.AppException
import com.wave.audiorecording.exception.PermissionDeniedException
import com.wave.audiorecording.exception.PlayerDataSourceException
import com.wave.audiorecording.player.PlayerContract.Player
import com.wave.audiorecording.player.PlayerContract.PlayerCallback
import com.wave.audiorecording.util.AppConstants
import java.io.IOException
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask

class AudioPlayer private constructor() : Player, OnPreparedListener {
    private val actionsListeners: MutableList<PlayerCallback> = ArrayList()
    private var mediaPlayer: MediaPlayer? = null
    private var timerProgress: Timer? = null
    private var isPrepared = false
    override var isPause = false
        private set
    override var pauseTime: Long = 0
        private set
    private var pausePos: Long = 0
    private var dataSource: String? = null
    override fun addPlayerCallback(callback: PlayerCallback?) {
        if (callback != null) {
            actionsListeners.add(callback)
        }
    }

    override fun removePlayerCallback(callback: PlayerCallback?): Boolean {
        return if (callback != null) {
            actionsListeners.remove(callback)
        } else false
    }

    override fun setData(data: String) {
        if (mediaPlayer != null && dataSource != null && dataSource == data) {
            //Do nothing
        } else {
            dataSource = data
            restartPlayer()
        }
    }

    private fun restartPlayer() {
        if (dataSource != null) {
            try {
                isPrepared = false
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(dataSource)
                mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            } catch (e: IOException) {
                if (e.message?.contains("Permission denied") == true) {
                    onError(PermissionDeniedException())
                } else {
                    onError(PlayerDataSourceException())
                }
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("Permission denied") == true) {
                    onError(PermissionDeniedException())
                } else {
                    onError(PlayerDataSourceException())
                }
            } catch (e: IllegalStateException) {
                if (e.message?.contains("Permission denied") == true) {
                    onError(PermissionDeniedException())
                } else {
                    onError(PlayerDataSourceException())
                }
            } catch (e: SecurityException) {
                if (e.message?.contains("Permission denied") == true) {
                    onError(PermissionDeniedException())
                } else {
                    onError(PlayerDataSourceException())
                }
            }
        }
    }

    override fun playOrPause() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    pause()
                } else {
                    isPause = false
                    if (!isPrepared) {
                        try {
                            mediaPlayer?.setOnPreparedListener(this)
                            mediaPlayer?.prepareAsync()
                        } catch (ex: IllegalStateException) {
                            restartPlayer()
                            mediaPlayer?.setOnPreparedListener(this)
                            try {
                                mediaPlayer?.prepareAsync()
                            } catch (e: IllegalStateException) {
                                restartPlayer()
                            }
                        }
                    } else {
                        mediaPlayer?.start()
                        mediaPlayer?.seekTo(pausePos.toInt())
                        onStartPlay()
                        mediaPlayer?.setOnCompletionListener {
                            stop()
                            onStopPlay()
                        }
                        timerProgress = Timer()
                        timerProgress?.schedule(object : TimerTask() {
                            override fun run() {
                                try {
                                    if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                                        val curPos = mediaPlayer?.currentPosition ?: 0
                                        onPlayProgress(curPos.toLong())
                                    }
                                } catch (e: IllegalStateException) {
                                    Log.e("TAG", "Player is not initialized!")
                                }
                            }
                        }, 0, AppConstants.VISUALIZATION_INTERVAL)
                    }
                    pausePos = 0
                }
            }
        } catch (e: IllegalStateException) {
            Log.e("TAG", "Player is not initialized!")
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        if (mediaPlayer !== mp) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = mp
        }
        onPreparePlay()
        isPrepared = true
        mediaPlayer?.start()
        mediaPlayer?.seekTo(pauseTime.toInt())
        onStartPlay()
        mediaPlayer?.setOnCompletionListener {
            stop()
            onStopPlay()
        }
        timerProgress = Timer()
        timerProgress?.schedule(object : TimerTask() {
            override fun run() {
                try {
                    if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                        val curPos = mediaPlayer?.currentPosition ?: 0
                        onPlayProgress(curPos.toLong())
                    }
                } catch (e: IllegalStateException) {
                    Log.e("TAG", "Player is not initialized!")
                }
            }
        }, 0, AppConstants.VISUALIZATION_INTERVAL)
    }

    override fun seek(mills: Long) {
        pauseTime = mills
        if (isPause) {
            pausePos = mills
        }
        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.seekTo(pauseTime.toInt())
                onSeek(pauseTime)
            }
        } catch (e: IllegalStateException) {
            Log.e("TAG", "Player is not initialized!")
        }
    }

    override fun pause() {
        if (timerProgress != null) {
            timerProgress?.cancel()
            timerProgress?.purge()
        }
        if (mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                onPausePlay()
                pauseTime = (mediaPlayer?.currentPosition ?: 0).toLong()
                isPause = true
                pausePos = pauseTime
            }
        }
    }

    override fun stop() {
        if (timerProgress != null) {
            timerProgress?.cancel()
            timerProgress?.purge()
        }
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.setOnCompletionListener(null)
            isPrepared = false
            onStopPlay()
            mediaPlayer?.currentPosition
            pauseTime = 0
        }
        isPause = false
        pausePos = 0
    }

    override val isPlaying: Boolean
        get() {
            try {
                return mediaPlayer != null && mediaPlayer?.isPlaying == true
            } catch (e: IllegalStateException) {
                Log.e("TAG", "Player is not initialized!")
            }
            return false
        }

    override fun release() {
        stop()
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
        isPrepared = false
        isPause = false
        dataSource = null
        actionsListeners.clear()
    }

    private fun onPreparePlay() {
        if (!actionsListeners.isEmpty()) {
            for (i in actionsListeners.indices) {
                actionsListeners[i].onPreparePlay()
            }
        }
    }

    private fun onStartPlay() {
        if (!actionsListeners.isEmpty()) {
            for (i in actionsListeners.indices) {
                actionsListeners[i].onStartPlay()
            }
        }
    }

    private fun onPlayProgress(mills: Long) {
        if (!actionsListeners.isEmpty()) {
            for (i in actionsListeners.indices) {
                actionsListeners[i].onPlayProgress(mills)
            }
        }
    }

    private fun onStopPlay() {
        if (!actionsListeners.isEmpty()) {
            for (i in actionsListeners.indices.reversed()) {
                actionsListeners[i].onStopPlay()
            }
        }
    }

    private fun onPausePlay() {
        if (!actionsListeners.isEmpty()) {
            for (i in actionsListeners.indices) {
                actionsListeners[i].onPausePlay()
            }
        }
    }

    private fun onSeek(mills: Long) {
        if (!actionsListeners.isEmpty()) {
            for (i in actionsListeners.indices) {
                actionsListeners[i].onSeek(mills)
            }
        }
    }

    private fun onError(throwable: AppException) {
        if (!actionsListeners.isEmpty()) {
            for (i in actionsListeners.indices) {
                actionsListeners[i].onError(throwable)
            }
        }
    }

    private object SingletonHolder {
        val singleton = AudioPlayer()
    }

    companion object {
        @JvmStatic
        val instance: AudioPlayer
            get() = SingletonHolder.singleton
    }
}