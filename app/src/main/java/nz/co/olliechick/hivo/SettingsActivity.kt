package nz.co.olliechick.hivo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
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
                intent.data = Uri.parse("mailto:" + getString(R.string.dev_email))
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                startActivity(intent)
                true
            }

            val filenamePreference = findPreference<ListPreference>("filename")
            val numberOfItems = FilenameFormat.values().size
            val now = Date()

            val entries = arrayOfNulls<String>(numberOfItems)
            activity?.applicationContext?.let { context ->
                arrayListOf<String>().apply {
                    FilenameFormat.values().forEach {
                        this.add(Util.getDateString(context, it, now) ?: getString(R.string.specify_on_save))
                    }
                }.toArray(entries)
            }
            filenamePreference?.entries = entries

            val values = arrayOfNulls<String>(numberOfItems)
            arrayListOf<String>().apply { FilenameFormat.values().forEach { this.add(it.name) } }
                .toArray(values)
            filenamePreference?.entryValues = values

            if (filenamePreference != null && filenamePreference.value == null) {
                val defaultValue = FilenameFormat.READABLE.name
                PreferenceManager.getDefaultSharedPreferences(activity).getString(filenamePreference.key, defaultValue)
                filenamePreference.value = defaultValue
            }

        }
    }
}