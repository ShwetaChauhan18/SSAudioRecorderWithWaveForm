package com.wave.audiorecording.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * RecordInfo
 */
@Parcelize
data class RecordInfo(
    val name: String?,
    val format: String?,
    val location: String?,
    val duration: Long,
    val created: Long,
    val size: Long
) : Parcelable