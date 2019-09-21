package nz.co.olliechick.hivo.util

import android.content.Context
import androidx.preference.PreferenceManager
import java.util.*

class Preferences {
    companion object {

        fun saveStartTime(context: Context) {
            val prefsEditor = getPrefs(context).edit()
            prefsEditor.putLong(Constants.startTimeKey, Date().time)
            prefsEditor.apply()
        }

        /** Returns maximum record time in minutes */
        fun getMaximumRecordTime(context: Context): Int =
            getPrefs(context).getString(Constants.bufferKey, null)?.toInt() ?: Constants.defaultBuffer


        fun getStartTime(context: Context): Date =
            Date().apply { time = getPrefs(context).getLong(Constants.startTimeKey, Date().time) }

        fun getFilename(context: Context): String? = getPrefs(context).getString(Constants.filenameKey, null) ?: null

        private fun getPrefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
    }

}
