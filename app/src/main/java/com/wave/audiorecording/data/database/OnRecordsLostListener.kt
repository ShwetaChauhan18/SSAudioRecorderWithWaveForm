package com.wave.audiorecording.data.database

/**
 * OnRecordsLostListener
 */
interface OnRecordsLostListener {
    fun onLostRecords(list: List<Record?>?)
}