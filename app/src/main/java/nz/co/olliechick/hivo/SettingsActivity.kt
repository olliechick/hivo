package nz.co.olliechick.hivo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

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
            activity?.applicationContext?.let {
                filenamePreference?.entries = arrayOf(
                    Util.getDateString(it, FilenameFormat.SORTABLE),
                    Util.getDateString(it, FilenameFormat.READABLE),
                    getString(R.string.specify_on_save)
                )
            }

            val defaultValue = FilenameFormat.READABLE.name
            filenamePreference?.entryValues = arrayOf(
                FilenameFormat.SORTABLE.name,
                defaultValue,
                FilenameFormat.SPECIFY_ON_SAVE.name
            )

            filenamePreference?.setDefaultValue(defaultValue)
        }
    }
}