package nz.co.olliechick.hivo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*


@Entity(tableName = "recordings")
class Recording(
    @PrimaryKey var name: String,
    @ColumnInfo(name = "start_date") var startDate: Calendar,
    @ColumnInfo(name = "end_date") var endDate: Calendar
) {
    fun schedule(applicationContext: Context) {
        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(applicationContext, SchedRecordingReceiver::class.java).let {
            PendingIntent.getBroadcast(applicationContext, 0, it, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //for KitKat and above, .set() is inexact
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, intent)
        } else alarmManager.set(AlarmManager.RTC_WAKEUP, startDate.timeInMillis, intent)
    }

    /**
     * Returns the date range of the recording.
     * Examples: "2PM → 3PM, 1 Sep", "2PM → 4:10 PM, 1 Sep", "2PM, 1 Sep → 10AM, 2 Sep".
     */
    private fun getDateRange(): String {
        val timeFormatMinutes = "h:mm a"
        val timeFormatNoMinutes = "ha"
        val datetimeFormatMinutes = "$timeFormatMinutes, d MMM"
        val datetimeFormatNoMinutes = "$timeFormatNoMinutes, d MMM"

        val sameDay = startDate.get(Calendar.DAY_OF_YEAR) == endDate.get(Calendar.DAY_OF_YEAR)
                && startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR)

        var timeFormat = timeFormatMinutes
        var datetimeFormat = datetimeFormatMinutes
        if (startDate.get(Calendar.MINUTE) == 0) {
            timeFormat = timeFormatNoMinutes
            datetimeFormat = datetimeFormatNoMinutes
        }

        var dateRange =
            if (sameDay) SimpleDateFormat(timeFormat, Locale.ENGLISH).format(startDate.time)
            else SimpleDateFormat(datetimeFormat, Locale.ENGLISH).format(startDate.time)
        dateRange += " → "

        datetimeFormat = if (endDate.get(Calendar.MINUTE) == 0) datetimeFormatNoMinutes
        else datetimeFormatMinutes

        dateRange += SimpleDateFormat(datetimeFormat, Locale.ENGLISH).format(endDate.time)

        return dateRange
    }

    override fun toString(): String = "$name: ${getDateRange()}"

    fun toHtml(): String = "<b>$name</b>: ${getDateRange()}"

}