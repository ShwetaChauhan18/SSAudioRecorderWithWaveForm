package com.wave.audiorecording.exception

class PermissionDeniedException : AppException() {
    override val type: Int
        get() = READ_PERMISSION_DENIED
}