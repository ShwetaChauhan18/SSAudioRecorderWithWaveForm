package com.wave.audiorecording.data.database

import java.util.Arrays

class Record {
    val id: Int
    private val name: String
    var duration: Long
    val created: Long
    val added: Long
    val removed: Long
    var path: String
    var isBookmarked: Boolean
        private set
    val isWaveformProcessed: Boolean
    val amps: IntArray
    val data: ByteArray

    //TODO: Remove not needed data clusters.
    constructor(id: Int, name: String, duration: Long, created: Long, added: Long, removed: Long, path: String,
                bookmark: Boolean, waveformProcessed: Boolean, amps: IntArray) {
        this.id = id
        this.name = name
        this.duration = duration
        this.created = created
        this.added = added
        this.removed = removed
        this.path = path
        isBookmarked = bookmark
        isWaveformProcessed = waveformProcessed
        this.amps = amps
        data = int2byte(amps)
        //		this.data = AndroidUtils.int2byte(amps);
    }

    constructor(id: Int, name: String, duration: Long, created: Long, added: Long, removed: Long, path: String,
                bookmark: Boolean, waveformProcessed: Boolean, amps: ByteArray) {
        this.id = id
        this.name = name
        this.duration = duration
        this.created = created
        this.added = added
        this.removed = removed
        this.path = path
        isBookmarked = bookmark
        isWaveformProcessed = waveformProcessed
        this.amps = byte2int(amps)
        data = amps
    }

    fun int2byte(amps: IntArray): ByteArray {
        val bytes = ByteArray(amps.size)
        for (i in amps.indices) {
            if (amps[i] >= 255) {
                bytes[i] = 127
            } else if (amps[i] < 0) {
                bytes[i] = 0
            } else {
                bytes[i] = (amps[i] - 128).toByte()
            }
        }
        return bytes
    }

    fun byte2int(amps: ByteArray): IntArray {
        val ints = IntArray(amps.size)
        for (i in amps.indices) {
            ints[i] = amps[i] + 128
        }
        return ints
    }

    fun getName(): String {
        return name
    }

    fun setBookmark(b: Boolean) {
        isBookmarked = b
    }

    override fun toString(): String {
        return "Record{" +
                "id=" + id + '\'' +
                ", name='" + name + '\'' +
                ", duration=" + duration + '\'' +
                ", created=" + created + '\'' +
                ", added=" + added + '\'' +
                ", path='" + path + '\'' +
                ", bookmark=" + isBookmarked + '\'' +
                ", waveformProcessed=" + isWaveformProcessed + '\'' +
                ", amps=" + Arrays.toString(amps) +
                ", data=" + Arrays.toString(data) +
                '}'
    }

    companion object {
        const val NO_ID = -1
    }
}