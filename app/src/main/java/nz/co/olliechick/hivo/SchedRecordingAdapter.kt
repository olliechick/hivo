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
    private val schedRecordings: ArrayList<String>
) : RecyclerView.Adapter<SchedRecordingAdapter.SchedRecordingViewHolder>() {
    override fun getItemCount(): Int {
        Log.i("FOO", "getItemCount")
        return schedRecordings.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchedRecordingViewHolder {
        Log.i("FOO", "onCreateViewHolder")
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.sched_recording_item, parent, false)
        val holder = SchedRecordingViewHolder(view)
        return holder
    }

    override fun onBindViewHolder(holder: SchedRecordingViewHolder, i: Int) {
        Log.i("FOO", "onBindViewHolder")
        holder.recordingName.text = schedRecordings[i]
    }

    class SchedRecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recordingName: TextView = view.findViewById(R.id.recordingName)
    }
}