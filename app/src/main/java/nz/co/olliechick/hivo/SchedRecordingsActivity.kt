package nz.co.olliechick.hivo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_sched_recordings.*
import org.jetbrains.anko.onClick
import org.jetbrains.anko.toast
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.schedule_recording_dialog.view.*
import nz.co.olliechick.hivo.Util.Companion.usesCustomFilename
import java.text.SimpleDateFormat
import java.util.*


class SchedRecordingsActivity : AppCompatActivity() {

    private var view: View? = null
    var recordings: ArrayList<String> = arrayListOf("Initial item")
        set(value) {
            Log.i("FOO", "set ${field[0]}")
            field = value
            val adapter = list.adapter
            list.adapter = SchedRecordingAdapter(this, field)
            adapter?.notifyDataSetChanged()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sched_recordings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.scheduled_recordings)

        fab.onClick {
            openScheduleRecordingDialog(
                Calendar.getInstance(),
                Calendar.getInstance().apply {
                    add(Calendar.MINUTE, Util.getMaximumRecordTime(this@SchedRecordingsActivity))
                }
            )
            val layoutManager = LinearLayoutManager(this)
            list.layoutManager = layoutManager
        }
    }

    @SuppressLint("InflateParams")
    private fun openScheduleRecordingDialog(startDate: Calendar, endDate: Calendar) {
        val builder = AlertDialog.Builder(this)
        view = layoutInflater.inflate(R.layout.schedule_recording_dialog, null)
        if (!usesCustomFilename(this)) {
            view!!.name.visibility = View.GONE
        }

        builder.setView(view)
        builder.setTitle(getString(R.string.schedule_recording))

        initialiseDateTimes(startDate, endDate)

        view?.startDateButton?.onClick {
            showDatePicker(this, startDate, ::setStartDate)
        }

        view?.endDateButton?.onClick {
            showDatePicker(this, endDate, ::setEndDate)
        }

        view?.startTimeButton?.onClick {
            showTimePicker(this, startDate, ::setStartTime)
        }

        view?.endTimeButton?.onClick {
            showTimePicker(this, endDate, ::setEndTime)
        }

        // Set up the bottom buttonbar
        builder.setPositiveButton(getString(R.string.schedule)) { _, _ ->
            run {
                toast("Scheduling...")
                recordings.add("new")
                list.adapter?.notifyDataSetChanged()
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }

        builder.create()
        builder.show()

    }

    private fun initialiseDateTimes(startDate: Calendar, endDate: Calendar) {
        setStartDate(startDate)
        setStartTime(startDate)
        setEndDate(endDate)
        setEndTime(endDate)
    }

    /**
     * Opens a date picker initialised to date.
     * It then calls setDateCallback with the datetime the user picked.
     */
    private fun showDatePicker(context: Context, date: Calendar, setDateCallback: (Calendar) -> Unit) {
        DatePickerDialog(context, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            date.apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthOfYear)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            setDateCallback(date)
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE)).run {
            show()
        }
    }

    private fun showTimePicker(context: Context, date: Calendar, setTimeCallback: (Calendar) -> Unit) {
        TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            date.apply {
                set(Calendar.HOUR, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
            setTimeCallback(date)
        }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), false).show()
    }

    private fun setDate(date: Calendar, dateButton: Button) {
        val format = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
        dateButton.text = format.format(date.time)
    }

    private fun setTime(time: Calendar, timeButton: Button) {
        val format = SimpleDateFormat("h:mm aa", Locale.ENGLISH)
        timeButton.text = format.format(time.time)
    }

    private fun setStartDate(date: Calendar) {
        view?.startDateButton?.run { setDate(date, this) }
    }

    private fun setEndDate(date: Calendar) {
        view?.endDateButton?.run { setDate(date, this) }
    }

    private fun setStartTime(date: Calendar) {
        view?.startTimeButton?.run { setTime(date, this) }
    }

    private fun setEndTime(date: Calendar) {
        view?.endTimeButton?.run { setTime(date, this) }
    }
}
