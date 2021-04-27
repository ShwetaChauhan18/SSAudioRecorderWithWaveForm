package com.wave.audiorecording.util

interface Contract {
    interface View {
        fun showProgress()
        fun hideProgress()
        fun showError(message: String?)
        fun showError(resId: Int)
        fun showMessage(resId: Int)
    }

    interface UserActionsListener<T : View?> {
        fun bindView(view: T)
        fun unbindView()
        fun clear()
    }
}