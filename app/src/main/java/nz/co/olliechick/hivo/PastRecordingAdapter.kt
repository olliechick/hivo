package nz.co.olliechick.hivo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PastRecordingAdapter(
    private val context: Context,
    private val pastRecordings: ArrayList<Recording>
) : RecyclerView.Adapter<PastRecordingAdapter.PastRecordingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastRecordingViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.past_recording_item, parent, false)
        return PastRecordingViewHolder(view)
    }

    override fun getItemCount() = pastRecordings.size

    override fun onBindViewHolder(holder: PastRecordingViewHolder, i: Int) {
        holder.recordingName.text = pastRecordings[i].name
    }

    class PastRecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recordingName: TextView = view.findViewById(R.id.recording_name)
    }

}
