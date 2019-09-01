package nz.co.olliechick.hivo.util

import android.content.Context
import androidx.preference.PreferenceManager
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
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val date = Date().apply { time = sharedPreferences.getLong(Constants.startTimeKey, Date().time) }
            return getNameForRecording(context, date)
        }

        /**
         * Returns a name based on stored settings for filename format and the passed-in time.
         * If filename format is custom, returns null.
         * Note that it will not include the .wav extension.
         * Example return: "2:59:35 PM, 1 Sep 2019"
         */
        fun getNameForRecording(context: Context, date: Date): String? {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val name = sharedPreferences.getString(Constants.filenameKey, null) ?: return null

            return getDateString(
                FilenameFormat.valueOf(
                    name
                ), date
            )
        }

        fun usesCustomFilename(context: Context) = getNameForCurrentRecording(context) == null

    }
}