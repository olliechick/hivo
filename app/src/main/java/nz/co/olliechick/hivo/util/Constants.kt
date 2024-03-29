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
        const val endTimeKey = "end_time"
        const val filenameKey = "filename" //also defined in root_preferences.xml
        const val bufferKey = "buffer"
        const val newAmplitudeIntent = "new_amplitude"
        const val recordingStartedIntent = "recording_started"
        const val recordingStoppedIntent = "recording_stopped"
        const val amplitudeKey = "amplitude"
        const val startsKey = "start"
        const val nameKey = "name"
        const val onboardingCompleteKey = "onboarding_complete"
        const val providerPath = "nz.co.olliechick.hivo.provider"

        // Default values
        const val defaultBuffer = 60 //minutes
        val defaultFilenameFormat = FilenameFormat.READABLE

        // Audio
        const val samplingRateHz = 44100
        const val numChannels = 2
        const val bitsPerSample = 16
        const val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        const val fileExt = ".wav"
        const val sizeOfWavHeader = 44
        const val blockAlign = numChannels * (bitsPerSample / 8)

        // UI
        const val invalidColour = Color.RED
        const val validColour = Color.BLACK

        // Links
        const val helpUrl = "https://docs.google.com/document/d/1Ayy6e52J_IaNXumw5bOuv1kslXrlIouXY6a_Ba71CyY"
        const val devEmailLink = "mailto:hivoapp@gmail.com"

        // Special numbers
        const val unsignedIntMaxValue = 4294967295 // 2^32 - 1
    }
}