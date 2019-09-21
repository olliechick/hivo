package nz.co.olliechick.hivo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import nz.co.olliechick.hivo.util.Constants.Companion.devEmailLink
import nz.co.olliechick.hivo.util.FilenameFormat
import nz.co.olliechick.hivo.util.StringProcessing.Companion.getDateString
import java.util.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            initSettings()
        }

        private fun initSettings() {
            findPreference<Preference>("feedback")?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse(devEmailLink)
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                startActivity(intent)
                true
            }

            val filenamePreference = findPreference<ListPreference>("filename")
            val numberOfItems = FilenameFormat.values().size
            val now = Date()

            val entries = arrayOfNulls<String>(numberOfItems)
            activity?.applicationContext?.let {
                arrayListOf<String>().apply {
                    FilenameFormat.values().forEach {
                        this.add(getDateString(it, now) ?: getString(R.string.specify_on_save))
                    }
                }.toArray(entries)
            }
            filenamePreference?.entries = entries

            val values = arrayOfNulls<String>(numberOfItems)
            arrayListOf<String>().apply { FilenameFormat.values().forEach { this.add(it.name) } }
                .toArray(values)
            filenamePreference?.entryValues = values

            // Set to filename preference to default if not assigned
            if (filenamePreference?.value == null) filenamePreference?.value = FilenameFormat.READABLE.name

        }
    }
}