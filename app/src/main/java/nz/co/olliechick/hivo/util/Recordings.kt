package nz.co.olliechick.hivo.util

import nz.co.olliechick.hivo.Recording
import java.util.*

class Recordings {
    companion object {

        /**
         * Returns a [List] of [Recording]s in [recordings] that will occur between [startDate]
         * and [endDate].
         * If there are none, this will be the empty list.
         */
        fun getIntersectingRecordings(
            recordings: List<Recording>,
            startDate: Calendar,
            endDate: Calendar
        ): List<Recording> {
            val intersectingRecordings: ArrayList<Recording> = arrayListOf()
            recordings.forEach {
                // RULE: if it starts before we've ended AND it ends after we start, it intersects
                if (it.startDate < endDate && it.endDate > startDate) intersectingRecordings.add(it)
            }
            return intersectingRecordings
        }
    }
}