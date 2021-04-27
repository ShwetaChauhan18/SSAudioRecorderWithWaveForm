package com.wave.audiorecording.exception

import com.wave.audiorecording.R

object ErrorParser {
    fun parseException(exception: AppException?): Int {
        exception?.let {e->
            if (e.type == AppException.Companion.CANT_CREATE_FILE) {
                return R.string.error_cant_create_file
            } else if (e.type == AppException.Companion.INVALID_OUTPUT_FILE) {
                return R.string.error_invalid_output_file
            } else if (e.type == AppException.Companion.RECORDER_INIT_EXCEPTION) {
                return R.string.error_failed_to_init_recorder
            } else if (e.type == AppException.Companion.PLAYER_DATA_SOURCE_EXCEPTION) {
                return R.string.error_player_data_source
            } else if (e.type == AppException.Companion.PLAYER_INIT_EXCEPTION) {
                return R.string.error_failed_to_init_player
            } else if (e.type == AppException.Companion.CANT_PROCESS_RECORD) {
                return R.string.error_process_waveform
            } else if (e.type == AppException.Companion.NO_SPACE_AVAILABLE) {
                return R.string.error_no_available_space
            } else if (e.type == AppException.Companion.RECORDING_ERROR) {
                return R.string.error_on_recording
            } else if (e.type == AppException.Companion.READ_PERMISSION_DENIED) {
                return R.string.error_permission_denied
            }
        }
        return R.string.error_unknown
    }
}