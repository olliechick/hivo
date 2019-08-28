package nz.co.olliechick.hivo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_sched_recordings.*
import kotlinx.android.synthetic.main.schedule_recording_dialog.view.*
import nz.co.olliechick.hivo.Util.Companion.usesCustomFilename
import org.jetbrains.anko.onClick
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*


class SchedRecordingsActivity : AppCompatActivity() {

    private var view: View? = null
    private lateinit var schedRecording: SchedRecording

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
        schedRecording = SchedRecording(startDate, endDate)

        val builder = AlertDialog.Builder(this)
        view = layoutInflater.inflate(R.layout.schedule_recording_dialog, null)
        if (!usesCustomFilename(this)) {
            view!!.name.visibility = View.GONE
        }

        builder.run {
            setView(view)
            setTitle(getString(R.string.schedule_recording))
            setPositiveButton(getString(R.string.schedule), null) // will be overridden
            setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        }

        initialiseDateTimes(startDate, endDate)

        view?.run {
            startDateButton?.onClick {
                showDatePicker(this@SchedRecordingsActivity, startDate, ::setStartDate)
            }

            endDateButton?.onClick {
                showDatePicker(this@SchedRecordingsActivity, endDate, ::setEndDate)
            }

            startTimeButton?.onClick {
                showTimePicker(this@SchedRecordingsActivity, startDate, ::setStartTime)
            }

            endTimeButton?.onClick {
                showTimePicker(this@SchedRecordingsActivity, endDate, ::setEndTime)
            }
        }

        val dialog = builder.create()

        // Override positive button, so that it only dismisses if validation passes
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                var filename = view?.name?.input?.text?.toString()
                if (filename == null || filename == "") filename = "(no title)"

                if (!schedRecording.hasValidDate()) {
                    val alertDialog = AlertDialog.Builder(this).create()
                    alertDialog.setMessage("Start time cannot be after the end time.")
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { dialog, _ -> dialog.dismiss() }
                    alertDialog.show()

                } else {
                    toast("Scheduling...")
                    recordings.add(filename) //todo fix
                    list.adapter?.notifyDataSetChanged()
                    schedRecording.schedule(applicationContext)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()

    }

    private fun initialiseDateTimes(startDate: Calendar, endDate: Calendar) {
        startDate.apply { set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        endDate.apply { set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        setStartDate(startDate)
        setStartTime(startDate)
        setEndDate(endDate)
        setEndTime(endDate)
    }

    /**
     * Opens a date picker initialised to date.
     * It then calls setDateCallback with the date the user picked.
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

    /**
     * Opens a time picker initialised to date.
     * It then calls setTimeCallback with the time the user picked.
     */
    private fun showTimePicker(context: Context, date: Calendar, setTimeCallback: (Calendar) -> Unit) {
        TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            date.apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
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
        schedRecording.startDate = date
        validate()
    }

    private fun setEndDate(date: Calendar) {
        view?.endDateButton?.run { setDate(date, this) }
        schedRecording.endDate = date
        validate()
    }

    private fun setStartTime(date: Calendar) {
        view?.startTimeButton?.run { setTime(date, this) }
        schedRecording.startDate = date
        validate()
    }

    private fun setEndTime(date: Calendar) {
        view?.endTimeButton?.run { setTime(date, this) }
        schedRecording.endDate = date
        validate()
    }

    private fun validate() {
        if (!schedRecording.hasValidDate()) {
            view?.startDateButton?.textColor = Util.invalidColour
            view?.startTimeButton?.textColor = Util.invalidColour
        } else {
            view?.startDateButton?.textColor = Util.validColour
            view?.startTimeButton?.textColor = Util.validColour
        }
    }
}