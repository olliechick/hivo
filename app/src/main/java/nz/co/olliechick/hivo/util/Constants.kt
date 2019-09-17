package nz.co.olliechick.hivo.util

import android.content.Context
import android.graphics.Color
import android.media.AudioFormat
import android.view.Gravity
import android.widget.Toast

class Constants {
    companion object {
        // Internal keys/action names
        const val startTimeKey = "start_time"
        const val filenameKey = "filename" //also defined in root_preferences.xml
        const val bufferKey = "buffer"
        const val newAmplitudeIntent = "new_amplitude"
        const val amplitudeKey = "amplitude"
        const val startsKey = "start"

        // Default values
        const val defaultBuffer = 60 //minutes

        // Audio
        const val samplingRateHz = 44100
        const val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        const val fileExt = ".wav"

        // UI
        const val invalidColour = Color.RED
        const val validColour = Color.BLACK

        // Links
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