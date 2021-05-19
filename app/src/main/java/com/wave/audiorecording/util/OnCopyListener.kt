package com.wave.audiorecording.util

/**
 * OnCopyListener
 */
interface OnCopyListener {
    val isCancel: Boolean
    fun onCopyProgress(percent: Int, progress: Long, total: Long)
    fun onCanceled()
    fun onCopyFinish()
}