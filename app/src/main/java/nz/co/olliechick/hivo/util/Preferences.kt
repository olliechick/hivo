package nz.co.olliechick.hivo.util

import android.content.Context
import androidx.preference.PreferenceManager
import nz.co.olliechick.hivo.util.Constants.Companion.defaultFilenameFormat
import java.util.*

class Preferences {
    companion object {

        private fun getPrefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

        // Setters

        fun saveStartTime(context: Context) =
            getPrefs(context).edit().putLong(Constants.startTimeKey, Date().time).apply()

        fun saveEndTime(context: Context) =
            getPrefs(context).edit().putLong(Constants.endTimeKey, Date().time).apply()

        fun setOnboardingIsComplete(context: Context) =
            getPrefs(context).edit().putBoolean(Constants.onboardingCompleteKey, true).apply()

        // Getters

        /** Returns maximum record time in minutes */
        fun getMaximumRecordTime(context: Context): Int =
            getPrefs(context).getString(Constants.bufferKey, null)?.toInt() ?: Constants.defaultBuffer

        fun getStartTime(context: Context): Date =
            Date().apply { time = getPrefs(context).getLong(Constants.startTimeKey, Date().time) }

        fun getEndTime(context: Context): Date =
            Date().apply { time = getPrefs(context).getLong(Constants.endTimeKey, Date().time) }

        fun getFilenameFormat(context: Context): FilenameFormat {
            val filenamePref = getPrefs(context).getString(Constants.filenameKey, defaultFilenameFormat.toString())
            return if (filenamePref == null) defaultFilenameFormat
            else FilenameFormat.valueOf(filenamePref)
        }

        fun isOnboardingComplete(context: Context): Boolean =
            getPrefs(context).getBoolean(Constants.onboardingCompleteKey, false)
    }

}
