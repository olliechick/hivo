package nz.co.olliechick.hivo

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_past_recording.*
import nz.co.olliechick.hivo.util.Database.Companion.initialiseDb
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import android.view.GestureDetector
import nz.co.olliechick.hivo.util.Ui.Companion.toast


class PastRecordingActivity : AppCompatActivity() {

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
                    toast("Opening ${recordings[position].name}...")
                }

                override fun onLongItemClick(view: View?, position: Int) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
        )
    }


    // adapted from https://stackoverflow.com/a/26196831/8355496
    class RecyclerItemClickListener(
        context: Context,
        recyclerView: RecyclerView,
        private val mListener: OnItemClickListener?
    ) : RecyclerView.OnItemTouchListener {
        interface OnItemClickListener {
            fun onItemClick(view: View, position: Int)
            fun onLongItemClick(view: View?, position: Int)
        }

        private var mGestureDetector: GestureDetector

        init {
            mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent) = true

                override fun onLongPress(e: MotionEvent) {
                    val child = recyclerView.findChildViewUnder(e.x, e.y)
                    if (child != null && mListener != null) {
                        mListener.onLongItemClick(child, recyclerView.getChildAdapterPosition(child))
                    }
                }
            })
        }

        override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
            val childView = view.findChildViewUnder(e.x, e.y)
            if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
                mListener.onItemClick(childView, view.getChildAdapterPosition(childView))
                return true
            }
            return false
        }

        override fun onTouchEvent(view: RecyclerView, motionEvent: MotionEvent) {}

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
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
