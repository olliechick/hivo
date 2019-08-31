package nz.co.olliechick.hivo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import nz.co.olliechick.hivo.Util.Companion.fileExists
import nz.co.olliechick.hivo.Util.Companion.wavExists
import java.util.*


class SchedRecording(var startDate: Calendar, var endDate: Calendar) {
    var name: String? = null

    fun schedule(applicationContext: Context) {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(applicationContext, SchedRecordingReceiver::class.java).let {
            PendingIntent.getBroadcast(applicationContext, 0, it, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //for KitKat and above, .set() is inexact
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, intent)
        } else alarmManager.set(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, intent)
    }

    fun hasValidDate() = startDate < endDate

    fun filenameExists(context: Context) = if (name == null) false else wavExists(name!!, context)

    fun generateFilename(context: Context): String? {
        if (name == null) return null
        var filename = "$name.wav"
        var i = 2
        while (fileExists(filename, context)) {
            filename = "$name ($i).wav"
            i++
        }

        return filename
    }
}