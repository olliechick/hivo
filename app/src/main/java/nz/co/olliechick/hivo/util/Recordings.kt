package nz.co.olliechick.hivo.util

import android.content.Context
import android.content.Intent
import android.os.Build
import nz.co.olliechick.hivo.Recording
import nz.co.olliechick.hivo.RecordingService
import org.jetbrains.anko.doAsync
import java.util.*

class Recordings {
    companion object {

        /**
         * Returns a [List] of [Recording]s in [recordings] that will occur between [startDate]
         * and [endDate].
         * If there are none, this will be the empty list.
         */
        fun getOverlappingRecordings(
            recordings: List<Recording>,
            startDate: Calendar,
            endDate: Calendar
        ): List<Recording> {
            val overlappingRecordings: ArrayList<Recording> = arrayListOf()
            recordings.forEach {
                // RULE: if it starts before we've ended AND it ends after we start, it overlaps
                if (it.startDate < endDate && it.endDate > startDate) overlappingRecordings.add(it)
            }
            return overlappingRecordings
        }

        fun startRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else context.startService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java)
            context.stopService(intent)
        }

        fun cancelCurrentStopRecordingIntent(applicationContext: Context) {
            doAsync {
                val db = Database.initialiseDb(applicationContext)
                val currentRecording = db.recordingDao().getCurrentlyOccurringScheduledRecording()
                if (currentRecording != null) {
                    currentRecording.cancel(applicationContext)
                    db.recordingDao().delete(currentRecording)
                }
                db.close()
            }
        }
    }
}