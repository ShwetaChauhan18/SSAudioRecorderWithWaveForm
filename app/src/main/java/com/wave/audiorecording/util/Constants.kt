package com.wave.audiorecording.util

interface Constants {
    interface Intents {
        companion object {
            const val NUMBER_0_POINT_299 = 0.299
            const val NUMBER_0_POINT_587 = 0.587
            const val NUMBER_0_POINT_114 = 0.114
            const val NUMBER_255 = 255.0
            const val ELEMENT_VIDEO_BUTTON_BG_THEME_COLOR = "element_video_button_bg_theme_color"
            const val ELEMENT_AUDIO_BUTTON_BG_THEME_COLOR = "element_audio_button_bg_theme_color"
            const val RECORDED_AUDIO_PATH = "RECORDED_AUDIO_PATH"
            const val DELAY_1000_MILI_SECOND = 1000
            const val RETURN_FILENAME = "return_filename"
        }
    }

    companion object {
        const val EMPTY_FILE_NAME = ""
    }
}