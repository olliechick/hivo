package nz.co.olliechick.hivo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_past_recording.*
import nz.co.olliechick.hivo.util.Database.Companion.initialiseDb
import nz.co.olliechick.hivo.util.Files.Companion.launchImplicitAudioIntent
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*


class PastRecordingsActivity : AppCompatActivity() {

    private lateinit var db: RecordingDatabase

    var recordings: ArrayList<Recording> = arrayListOf()
        set(value) {
            field = value
            list.adapter = PastRecordingAdapter(this, field)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_past_recording)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.past_recordings)

        populateList()

        list.addOnItemTouchListener(
            RecyclerItemClickListener(this, list, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    launchImplicitAudioIntent(this@PastRecordingsActivity, recordings[position].name)

                }

                override fun onLongItemClick(view: View?, position: Int) {
                    //todo open menu
                }
            })
        )
    }

    private fun populateList() {
        val layoutManager = LinearLayoutManager(this)
        list.layoutManager = layoutManager

        recordings = arrayListOf()

        doAsync {
            db = initialiseDb(applicationContext)
            val pastRecordings = db.recordingDao().getPastRecordings()
            uiThread {
                recordings = ArrayList(pastRecordings)
                if (recordings.size == 0) {
                    list.visibility = View.GONE
                    //todo empty_view.visibility = View.VISIBLE
                }
            }
            db.close()
        }
    }
}
