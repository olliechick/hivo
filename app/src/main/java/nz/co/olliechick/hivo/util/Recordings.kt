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
    }
}