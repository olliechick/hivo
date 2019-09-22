package nz.co.olliechick.hivo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import nz.co.olliechick.hivo.util.Constants.Companion.nameKey
import nz.co.olliechick.hivo.util.Constants.Companion.startsKey
import nz.co.olliechick.hivo.util.StringProcessing.Companion.formatDateRange
import nz.co.olliechick.hivo.util.toInt
import java.util.*

@Entity(tableName = "recordings")
class Recording(
    var name: String,
    @ColumnInfo(name = "start_date") var startDate: Calendar,
    @ColumnInfo(name = "end_date") var endDate: Calendar
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    private fun generatePendingIntent(applicationContext: Context, starts: Boolean): PendingIntent {
        val intent = Intent(applicationContext, SchedRecordingReceiver::class.java)
        intent.putExtra(startsKey, starts)
        intent.putExtra(nameKey, name)
        val requestCode = id.toInt() * 2 + starts.toInt() // This has to be unique for each pending intent
        return PendingIntent.getBroadcast(applicationContext, requestCode, intent, 0)
    }

    private fun generateStartPendingIntent(applicationContext: Context) =
        generatePendingIntent(applicationContext, true)

    private fun generateStopPendingIntent(applicationContext: Context) =
        generatePendingIntent(applicationContext, false)

    private fun getAlarmManager(applicationContext: Context) =
        applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(applicationContext: Context) {
        val alarmManager = getAlarmManager(applicationContext)
        val startPendingIntent = generateStartPendingIntent(applicationContext)
        val stopPendingIntent = generateStopPendingIntent(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //for KitKat and above, .set() is inexact
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, startPendingIntent)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, endDate.timeInMillis, stopPendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, startPendingIntent)
            alarmManager.set(AlarmManager.RTC_WAKEUP, endDate.timeInMillis, stopPendingIntent)
        }
    }

    fun cancel(applicationContext: Context) {
        val alarmManager = getAlarmManager(applicationContext)
        val startPendingIntent = generateStartPendingIntent(applicationContext)
        val stopPendingIntent = generateStopPendingIntent(applicationContext)

        alarmManager.cancel(startPendingIntent)
        alarmManager.cancel(stopPendingIntent)
    }

    private fun getDateRange() = formatDateRange(startDate, endDate)

    override fun toString(): String = "$name: ${getDateRange()}"

    fun toHtml(): String = "<b>$name</b>: ${getDateRange()}"

}