package nz.co.olliechick.hivo

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nz.co.olliechick.hivo.util.Database
import org.jetbrains.anko.doAsync
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set

class SchedRecordingAdapter(
    private val context: Context,
    private val schedRecordings: ArrayList<Recording>
) : RecyclerView.Adapter<SchedRecordingAdapter.SchedRecordingViewHolder>() {

    private val PENDING_REMOVAL_TIMEOUT = 3000 // 3sec

    var itemsPendingRemoval: ArrayList<Recording> = ArrayList()

    private val handler = Handler() // hanlder for running delayed runnables
    // map of items to pending runnables, so we can cancel a removal if need be
    private var pendingRunnables = HashMap<Recording, Runnable>()


    override fun getItemCount() = schedRecordings.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchedRecordingViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.sched_recording_item, parent, false)
        return SchedRecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: SchedRecordingViewHolder, i: Int) {
        val recording = schedRecordings[i]

        if (itemsPendingRemoval.contains(recording)) {
            // we need to show the "undo" state of the row
            holder.itemView.setBackgroundColor(Color.RED)
            holder.recordingName.visibility = View.GONE
            holder.undoButton.visibility = View.VISIBLE
            holder.undoButton.setOnClickListener {
                // user wants to undo the removal, let's cancel the pending task
                val pendingRemovalRunnable = pendingRunnables[recording]
                pendingRunnables.remove(recording)
                if (pendingRemovalRunnable != null) handler.removeCallbacks(pendingRemovalRunnable)
                itemsPendingRemoval.remove(recording)
                // this will rebind the row in "normal" state
                notifyItemChanged(schedRecordings.indexOf(recording))
            }
        } else {
            // we need to show the "normal" state
            holder.itemView.setBackgroundColor(Color.WHITE)
            holder.recordingName.visibility = View.VISIBLE
            holder.recordingName.text = recording.name
            holder.undoButton.visibility = View.GONE
            holder.undoButton.setOnClickListener(null)
        }
    }

    fun pendingRemoval(i: Int, applicationContext: Context) {
        val recording = schedRecordings[i]
        if (!itemsPendingRemoval.contains(recording)) {
            itemsPendingRemoval.add(recording)
            notifyItemChanged(i) // this will redraw row in "undo" state

            // let's create, store and post a runnable to remove the item
            val pendingRemovalRunnable =
                Runnable { remove(schedRecordings.indexOf(recording), applicationContext) }
            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT.toLong())
            pendingRunnables[recording] = pendingRemovalRunnable
        }
    }

    private fun remove(i: Int, applicationContext: Context) {
        val recording = schedRecordings[i]
        if (itemsPendingRemoval.contains(recording)) itemsPendingRemoval.remove(recording)
        if (schedRecordings.contains(recording)) {
            schedRecordings.removeAt(i)
            notifyItemRemoved(i)
        }
        recording.cancel(applicationContext)
        doAsync {
            Database.initialiseDb(applicationContext).apply {
                recordingDao().delete(recording)
                close()
            }
        }
    }

    fun isPendingRemoval(i: Int) = itemsPendingRemoval.contains(schedRecordings[i])

    class SchedRecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recordingName: TextView = view.findViewById(R.id.recording_name)
        val undoButton: Button = view.findViewById(R.id.undo_button)
    }
}