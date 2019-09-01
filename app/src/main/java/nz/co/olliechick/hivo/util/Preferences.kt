package nz.co.olliechick.hivo.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.util.*

class Preferences{
    companion object {

        /** Returns maximum record time in minutes */
        fun getMaximumRecordTime(context: Context): Int =
            PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.bufferKey,
                null
            )?.toInt()
                ?: Constants.defaultBuffer

        fun saveStartTime(prefs: SharedPreferences) {
            val prefsEditor = prefs.edit()
            prefsEditor.putLong(Constants.startTimeKey, Date().time)
            prefsEditor.apply()
        }

    }
}