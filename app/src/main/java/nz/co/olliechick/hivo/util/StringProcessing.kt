package nz.co.olliechick.hivo.util

import android.content.Context
import nz.co.olliechick.hivo.util.Preferences.Companion.getFilename
import nz.co.olliechick.hivo.util.Preferences.Companion.getStartTime
import java.text.SimpleDateFormat
import java.util.*

class StringProcessing {
    companion object {

        /**
         * Formats the passed in date using the passed in FilenameFormat, and returns it.
         * If the passed in FilenameFormat is SPECIFY_ON_SAVE (or null), returns null.
         */
        fun getDateString(format: FilenameFormat, date: Date): String? {
            val pattern = when (format) {
                FilenameFormat.READABLE -> "h:mm:ss a, d MMM yyyy"
                FilenameFormat.READABLE_SHORT -> "h:mm a, d MMM yyyy"
                FilenameFormat.SORTABLE -> "yyyy-MM-dd-HH-mm-ss"
                else -> null
            }
            return if (pattern == null) null
            else SimpleDateFormat(pattern, Locale.ENGLISH).format(date)
        }

        /**
         * Returns a name based on stored settings for filename format and start time.
         * If filename format is custom, returns null.
         * Note that it will not include the .wav extension.
         * Example return: "2:59:35 PM, 1 Sep 2019"
         */
        fun getNameForCurrentRecording(context: Context): String? {
            val date = getStartTime(context)
            return getNameForRecording(context, date)
        }

        /**
         * Returns a name based on stored settings for filename format and the passed-in time.
         * If filename format is custom, returns null.
         * Note that it will not include the .wav extension.
         * Example return: "2:59:35 PM, 1 Sep 2019"
         */
        fun getNameForRecording(context: Context, date: Date): String? {
            val name = getFilename(context)
            return if (name == null) null
            else getDateString(FilenameFormat.valueOf(name), date)
        }

        /**
         * Returns true if the filename format setting is set to custom.
         */
        fun usesCustomFilename(context: Context) = getNameForCurrentRecording(context) == null

        /**
         * Returns the date range startDate → endDate formatted compactly.
         * Examples: "2PM → 3PM, 1 Sep", "2PM → 4:10 PM, 1 Sep", "2PM, 1 Sep → 10AM, 2 Sep".
         */
        fun formatDateRange(startDate: Calendar, endDate: Calendar): String {
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

        fun generateUniqueName(name: String, nameExists: (String) -> Boolean): String {
            var filename = name
            var i = 2
            while (nameExists(filename)) {
                filename = "$name ($i)"
                i++
            }
            return filename
        }

        /**
         * Returns the time portion of a Date formatted h:mm.
         */
        fun getTimeString(date: Date): String = SimpleDateFormat("h:mm", Locale.US).format(date)

    }
}