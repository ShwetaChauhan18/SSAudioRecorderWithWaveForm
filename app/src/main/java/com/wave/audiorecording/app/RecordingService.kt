package com.wave.audiorecording.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.wave.audiorecording.AudioRecordingWavesApplication
import com.wave.audiorecording.R
import com.wave.audiorecording.data.FileRepository
import com.wave.audiorecording.data.database.Record
import com.wave.audiorecording.exception.AppException
import com.wave.audiorecording.record.RecordNewActivity
import com.wave.audiorecording.util.AndroidUtils
import com.wave.audiorecording.util.AppConstants
import java.io.File

class RecordingService : Service() {
    private val builder: Notification.Builder? = null
    private val notificationManager: NotificationManager? = null
    private val remoteViewsSmall: RemoteViews? = null
    private val notification: Notification? = null
    private var appRecorder: AppRecorder? = null
    private var appRecorderCallback: AppRecorderCallback? = null
    private var started = false
    private var fileRepository: FileRepository? = null
    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        appRecorder = AudioRecordingWavesApplication.injector?.provideAppRecorder()
        fileRepository = AudioRecordingWavesApplication.injector?.provideFileRepository()
        appRecorderCallback = object : AppRecorderCallback {
            override fun onRecordingStarted(file: File?) {
                updateNotificationResume()
            }

            override fun onRecordingPaused() {
                updateNotificationPause()
            }

            override fun onRecordProcessing() {}
            override fun onRecordFinishProcessing() {}
            override fun onRecordingStopped(file: File?, rec: Record?) {}
            override fun onRecordingProgress(mills: Long, amp: Int) {
                if (mills % (5 * AppConstants.VISUALIZATION_INTERVAL * AppConstants.SHORT_RECORD_DP_PER_SECOND) == 0L && fileRepository?.hasAvailableSpace(applicationContext)?.not() == true) {
                    AndroidUtils.runOnUIThread({
                        stopRecording()
                        Toast.makeText(applicationContext, R.string.error_no_available_space, Toast.LENGTH_LONG).show()
                    })
                }
            }

            override fun onError(throwable: AppException?) {}
        }
        appRecorder?.addRecordingCallback(appRecorderCallback)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null && !action.isEmpty()) {
                when (action) {
                    ACTION_START_RECORDING_SERVICE -> {
                    }
                    ACTION_STOP_RECORDING_SERVICE -> stopForegroundService()
                    ACTION_STOP_RECORDING -> stopRecording()
                    ACTION_PAUSE_RECORDING -> if (appRecorder?.isPaused == true) {
                        appRecorder?.resumeRecording()
                        updateNotificationResume()
                    } else {
                        appRecorder?.pauseRecording()
                        updateNotificationPause()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopRecording() {
        appRecorder?.stopRecording()
        stopForegroundService()
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(applicationContext, RecordNewActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
        return PendingIntent.getActivity(applicationContext, 0, intent, 0)
    }

    private fun stopForegroundService() {
        appRecorder?.removeRecordingCallback(appRecorderCallback)
        stopForeground(true)
        stopSelf()
        started = false
    }

    protected fun getPendingSelfIntent(context: Context?, action: String?): PendingIntent {
        val intent = Intent(context, StopRecordingReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(context, 10, intent, 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = notificationManager?.getNotificationChannel(channelId)
        if (channel == null) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            chan.setSound(null, null)
            chan.enableLights(false)
            chan.enableVibration(false)
            notificationManager?.createNotificationChannel(chan)
        }
        return channelId
    }

    private fun updateNotificationPause() {
        if (started && remoteViewsSmall != null) {
            notificationManager?.notify(NOTIF_ID, notification)
        }
    }

    private fun updateNotificationResume() {
        if (started && remoteViewsSmall != null) {
            notificationManager?.notify(NOTIF_ID, notification)
        }
    }

    class StopRecordingReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stopIntent = Intent(context, RecordingService::class.java)
            stopIntent.action = intent.action
            context.startService(stopIntent)
        }
    }

    companion object {
        const val ACTION_START_RECORDING_SERVICE = "ACTION_START_RECORDING_SERVICE"
        const val ACTION_STOP_RECORDING_SERVICE = "ACTION_STOP_RECORDING_SERVICE"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "ACTION_PAUSE_RECORDING"
        private const val CHANNEL_NAME = "Default"
        private const val CHANNEL_ID = "NotificationId"
        private const val CHANNEL_NAME_ERRORS = "Errors"
        private const val CHANNEL_ID_ERRORS = "Errors"
        private const val NOTIF_ID = 101
    }
}