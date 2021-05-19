package com.wave.audiorecording.util

interface Constants {
    interface Intents {
        companion object {
            const val PRIMARY_COLOUR = "primary_colour"
            const val TEXT_SIZE_17 = 17
            const val NUMBER_0_POINT_299 = 0.299
            const val NUMBER_0_POINT_587 = 0.587
            const val NUMBER_0_POINT_114 = 0.114
            const val NUMBER_255 = 255.0
            const val GALLERY_LIMIT = "gallery_limit"
            const val ELEMENT_POSITION = "gallery_position"
            const val ELEMENT_BUTTON_BG_THEME_COLOR = "element_button_bg_theme_color"
            const val ELEMENT_CAPTURE_BUTTON_BG_THEME_COLOR = "element_capture_button_bg_theme_color"
            const val ELEMENT_VIDEO_BUTTON_BG_THEME_COLOR = "element_video_button_bg_theme_color"
            const val ELEMENT_AUDIO_BUTTON_BG_THEME_COLOR = "element_audio_button_bg_theme_color"
            const val ELEMENT_BUTTON_THEME_COLOR = "element_button_theme_color"
            const val GALLERY_SINGLE = "gallery_single"
            const val RECORDED_AUDIO_PATH = "RECORDED_AUDIO_PATH"
            const val RECORDED_VIDEO_PATH = "RECORDED_VIDEO_PATH"
            const val DELAY_1000_MILI_SECOND = 1000
            const val RETURN_FILENAME = "return_filename"
            const val IS_DRAW_DATA = "is_draw_data"
            const val RETURN_FILENAMES_ARRAY = "return_filenames_array"
            const val RETURN_REPLACE = "return_replace"
            const val RETURN_SIGNED = "return_signed"
            const val RETURN_LOCATION = "return_location"
        }
    }

    companion object {
        const val EMPTY_FILE_NAME = ""
    }
}