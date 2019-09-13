package nz.co.olliechick.hivo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nz.co.olliechick.hivo.util.Recordings.Companion.startRecording


class SchedRecordingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        startRecording(context)
    }
}
