package com.wave.audiorecording.util

class IntArrayList {
    private var data = IntArray(100)
    private var size = 0
    fun add(`val`: Int) {
        if (data.size == size) {
            grow()
            add(`val`)
        }
        data[size] = `val`
        size++
    }

    operator fun get(index: Int): Int {
        return data[index]
    }

    fun getData(): IntArray {
        val arr = IntArray(size)
        for (i in 0 until size) {
            arr[i] = data[i]
        }
        return arr
    }

    fun clear() {
        data = IntArray(100)
        size = 0
    }

    fun size(): Int {
        return size
    }

    private fun grow() {
        val backup = data
        data = IntArray(size * 2)
        for (i in backup.indices) {
            data[i] = backup[i]
        }
    }
}