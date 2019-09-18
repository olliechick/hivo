package nz.co.olliechick.hivo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nz.co.olliechick.hivo.util.Constants.Companion.nameKey
import nz.co.olliechick.hivo.util.Constants.Companion.startsKey
import nz.co.olliechick.hivo.util.Files.Companion.saveWav
import nz.co.olliechick.hivo.util.Recordings.Companion.startRecording
import nz.co.olliechick.hivo.util.Recordings.Companion.stopRecording


class SchedRecordingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val start = intent.getBooleanExtra(startsKey, false)
        if (start) {
            startRecording(context) //todo make sure that the switch turns on even when this runs while app is open
        } else {
            stopRecording(context)
            val name = intent.getStringExtra(nameKey)
            saveWav(name, context)
        }
    }
}
