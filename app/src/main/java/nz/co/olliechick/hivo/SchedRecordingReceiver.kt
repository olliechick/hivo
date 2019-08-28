package nz.co.olliechick.hivo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class SchedRecordingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("FOO", "Received message ${intent.action}!")
        // todo create notification

        // todo start recording
    }
}
