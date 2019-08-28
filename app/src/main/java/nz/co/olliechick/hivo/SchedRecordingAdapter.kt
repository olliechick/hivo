package nz.co.olliechick.hivo

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SchedRecordingAdapter(
    private val context: Context,
    private val schedRecordings: List<String>
) : RecyclerView.Adapter<SchedRecordingAdapter.SchedRecordingViewHolder>() {

    override fun getItemCount() = schedRecordings.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchedRecordingViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.sched_recording_item, parent, false)
        return SchedRecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: SchedRecordingViewHolder, i: Int) {
        holder.recordingName.text = schedRecordings[i]
    }

    class SchedRecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recordingName: TextView = view.findViewById(R.id.recordingName)
    }
}