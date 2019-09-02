package nz.co.olliechick.hivo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import nz.co.olliechick.hivo.util.StringProcessing.Companion.formatDateRange
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

    private fun getDateRange() = formatDateRange(startDate, endDate)

    override fun toString(): String = "$name: ${getDateRange()}"

    fun toHtml(): String = "<b>$name</b>: ${getDateRange()}"

}