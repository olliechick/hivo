package nz.co.olliechick.hivo

import android.util.Log
import java.util.Calendar

class SchedRecording(var startDate: Calendar, var endDate: Calendar) {

    fun schedule() {
        Log.i("FOO", "${startDate.get(Calendar.HOUR_OF_DAY)} ${endDate.get(Calendar.HOUR_OF_DAY)}")
        //todo
    }

    fun hasValidDate() = startDate < endDate
}