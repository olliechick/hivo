package nz.co.olliechick.hivo.util

import android.content.Context
import androidx.preference.PreferenceManager
import java.util.*

class Preferences {
    companion object {

        private fun getPrefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

        // Setters

        fun saveStartTime(context: Context) =
            getPrefs(context).edit().putLong(Constants.startTimeKey, Date().time).apply()

        fun setOnboardingIsComplete(context: Context) =
            getPrefs(context).edit().putBoolean(Constants.onboardingCompleteKey, true).apply()

        // Getters

        /** Returns maximum record time in minutes */
        fun getMaximumRecordTime(context: Context): Int =
            getPrefs(context).getString(Constants.bufferKey, null)?.toInt() ?: Constants.defaultBuffer

        fun getStartTime(context: Context): Date =
            Date().apply { time = getPrefs(context).getLong(Constants.startTimeKey, Date().time) }

        fun getFilename(context: Context): String? = getPrefs(context).getString(Constants.filenameKey, null) ?: null

        fun isOnboardingComplete(context: Context): Boolean =
            getPrefs(context).getBoolean(Constants.onboardingCompleteKey, false)
    }

}
