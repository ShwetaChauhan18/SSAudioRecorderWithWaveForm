package com.wave.audiorecording

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.concurrent.CountDownLatch

class BackgroundQueue(threadName: String?) : Thread() {
    @Volatile
    private var handler: Handler? = null
    private val countDownLatch = CountDownLatch(1)

    @JvmOverloads
    fun postRunnable(runnable: Runnable?, delay: Long = 0) {
        try {
            countDownLatch.await()
            if (delay <= 0) {
                runnable?.let { handler?.post(it) }
            } else {
                runnable?.let { handler?.postDelayed(it, delay) }
            }
        } catch (e: Exception) {
            Log.e("TAG", e.message.toString())
        }
    }

    fun cleanupQueue() {
        try {
            countDownLatch.await()
            handler?.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e("TAG", e.message.toString())
        }
    }

    fun handleMessage(inputMessage: Message?) {}
    fun close() {
        handler?.looper?.quit()
    }

    override fun run() {
        Looper.prepare()
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                this@BackgroundQueue.handleMessage(msg)
            }
        }
        countDownLatch.countDown()
        Looper.loop()
    }

    init {
        name = threadName
        start()
    }
}