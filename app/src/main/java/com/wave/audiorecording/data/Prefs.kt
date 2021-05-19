package com.wave.audiorecording.data

interface Prefs {
    val isFirstRun: Boolean
    fun firstRunExecuted()
    var isStoreDirPublic: Boolean
    var isAskToRenameAfterStopRecording: Boolean
    fun hasAskToRenameAfterStopRecordingSetting(): Boolean
    var activeRecord: Long
    val recordCounter: Long
    fun incrementRecordCounter()
    fun setRecordInStereo(stereo: Boolean)
    val recordChannelCount: Int
    var isKeepScreenOn: Boolean
    var format: Int
    var bitrate: Int
    var sampleRate: Int
    fun setRecordOrder(order: Int)
    val recordsOrder: Int
    var namingFormat: Int
}