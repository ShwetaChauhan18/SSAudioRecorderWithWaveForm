package com.wave.audiorecording.app

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.exception.AppException
import com.wave.audiorecording.player.PlayerContract.Player
import com.wave.audiorecording.player.PlayerContract.PlayerCallback

class PlaybackService : Service() {
    private val builder: Notification.Builder? = null
    private val notificationManager: NotificationManager? = null
    private val remoteViewsSmall: RemoteViews? = null
    private val notification: Notification? = null
    private var recordName: String? = ""
    private var audioPlayer: Player? = null
    private var playerCallback: PlayerCallback? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        audioPlayer = AudioRecordingWavesApplication.injector?.provideAudioPlayer()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null && !action.isEmpty()) {
                when (action) {
                    ACTION_START_PLAYBACK_SERVICE -> if (intent.hasExtra(EXTRAS_KEY_RECORD_NAME)) {
                        startForeground(intent.getStringExtra(EXTRAS_KEY_RECORD_NAME))
                    }
                    ACTION_PAUSE_PLAYBACK -> audioPlayer?.playOrPause()
                    ACTION_CLOSE -> {
                        audioPlayer?.stop()
                        stopForegroundService()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun startForeground(name: String?) {
        recordName = name
        if (playerCallback == null) {
            playerCallback = object : PlayerCallback {
                override fun onPreparePlay() {}
                override fun onPlayProgress(mills: Long) {}
                override fun onStopPlay() {
                    stopForegroundService()
                }

                override fun onSeek(mills: Long) {}
                override fun onError(throwable: AppException?) {
                    stopForegroundService()
                }

                override fun onStartPlay() {
                    onStartPlayback()
                }

                override fun onPausePlay() {
                    onPausePlayback()
                }
            }
        }
        audioPlayer?.addPlayerCallback(playerCallback)
    }

    fun stopForegroundService() {
        audioPlayer?.removePlayerCallback(playerCallback)
        stopForeground(true)
        stopSelf()
    }

    fun onPausePlayback() {
        if (remoteViewsSmall != null) {
            builder?.setOngoing(false)
            notificationManager?.notify(NOTIF_ID, notification)
        }
    }

    fun onStartPlayback() {
        if (remoteViewsSmall != null) {
            builder?.setOngoing(true)
            notificationManager?.notify(NOTIF_ID, notification)
        }
    }

    class StopPlaybackReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stopIntent = Intent(context, PlaybackService::class.java)
            stopIntent.action = intent.action
            context.startService(stopIntent)
        }
    }

    companion object {
        const val ACTION_START_PLAYBACK_SERVICE = "ACTION_START_PLAYBACK_SERVICE"
        const val ACTION_PAUSE_PLAYBACK = "ACTION_PAUSE_PLAYBACK"
        const val ACTION_CLOSE = "ACTION_CLOSE"
        const val EXTRAS_KEY_RECORD_NAME = "record_name"
        private const val CHANNEL_ID = "NotificationId"
        private const val NOTIF_ID = 101
        private const val REQUEST_CODE = 10
        fun startServiceForeground(context: Context, name: String?) {
            val intent = Intent(context, PlaybackService::class.java)
            intent.action = ACTION_START_PLAYBACK_SERVICE
            intent.putExtra(EXTRAS_KEY_RECORD_NAME, name)
            context.startService(intent)
        }
    }
}