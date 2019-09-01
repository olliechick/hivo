package nz.co.olliechick.hivo.util

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Toast

class Constants {
    companion object {
        const val prefsFile = "me.olliechick.instagramunfollowers.prefs"
        const val startTimeKey = "start_time"
        const val filenameKey = "filename" //also defined in root_preferences.xml
        const val bufferKey = "buffer"
        const val defaultBuffer = 60 //minutes
        const val invalidColour = Color.RED
        const val validColour = Color.BLACK
        const val fileExt = ".wav"
        const val helpUrl =
            "https://docs.google.com/document/d/1Ayy6e52J_IaNXumw5bOuv1kslXrlIouXY6a_Ba71CyY"
        const val devEmailLink = "mailto:hivoapp@gmail.com"


        /**
         * This is just for debug purposes, and should be removed for the delivered product. todo
         */
        fun debugToast(context: Context, text: String) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).run {
                setGravity(Gravity.CENTER, 0, 300)
                show()
            }
        }
    }
}