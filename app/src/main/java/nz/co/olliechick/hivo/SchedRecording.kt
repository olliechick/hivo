package nz.co.olliechick.hivo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*


class SchedRecording(var startDate: Calendar, var endDate: Calendar) {
    fun schedule(applicationContext: Context) {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(applicationContext, SchedRecordingReceiver::class.java).let {
            PendingIntent.getBroadcast(applicationContext, 0, it, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //for KK and above, .set() is inexact
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, intent)
        } else alarmManager.set(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, intent)
    }

    fun hasValidDate() = startDate < endDate
}