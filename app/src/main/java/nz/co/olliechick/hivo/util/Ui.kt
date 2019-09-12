package nz.co.olliechick.hivo.util

import android.content.Context
import android.util.Log
import org.jetbrains.anko.toast as ankoToast

class Ui {
    companion object {
        fun Context.toast(message: CharSequence) {
            Log.i("HiVo toast", message.toString())
            ankoToast(message)
        }
    }
}