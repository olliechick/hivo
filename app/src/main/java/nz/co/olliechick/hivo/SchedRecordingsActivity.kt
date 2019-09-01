package nz.co.olliechick.hivo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_sched_recordings.*
import kotlinx.android.synthetic.main.schedule_recording_dialog.view.*
import nz.co.olliechick.hivo.Util.Companion.getIntersectingRecordings
import nz.co.olliechick.hivo.Util.Companion.getNameForRecording
import nz.co.olliechick.hivo.Util.Companion.initialiseDb
import nz.co.olliechick.hivo.Util.Companion.usesCustomFilename
import org.jetbrains.anko.*
import java.text.SimpleDateFormat
import java.util.*


class SchedRecordingsActivity : AppCompatActivity() {

    private var view: View? = null
    private lateinit var db: RecordingDatabase

    // start and end date initialised to the epoch to easily spot bugs, as these should be
    // set to whatever openScheduleRecordingDialog() is called with before being displayed.
    private var startDate: Calendar = GregorianCalendar(1970, 0, 1)
    private var endDate: Calendar = GregorianCalendar(1970, 0, 1)


    var recordings: ArrayList<Recording> = arrayListOf()
        set(value) {
            field = value
            list.adapter = SchedRecordingAdapter(this, field)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sched_recordings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.scheduled_recordings)

        populateList()

        fab.onClick {
            //Start at next full hour eg (2pm, 2:59:59pm) -> 3pm, and finish [max record time] later
            val start = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 60)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).also {
                it.add(Calendar.MINUTE, Util.getMaximumRecordTime(this))
            }
            openScheduleRecordingDialog(start, end)
        }
    }

    private fun populateList() {
        val layoutManager = LinearLayoutManager(this)
        list.layoutManager = layoutManager
        recordings = arrayListOf()

        doAsync {
            db = initialiseDb(applicationContext)
            val allRecordings = db.recordingDao().getAll()
            uiThread {
                recordings = ArrayList(allRecordings)
                if (recordings.size == 0) {
                    list.visibility = View.GONE
                    //todo empty_view.visibility = View.VISIBLE
                }
            }
            db.close()
        }

        val decoration = DividerItemDecoration(this, layoutManager.orientation)
        list.addItemDecoration(decoration)
    }

    private fun scheduleRecording(name: String) {
        toast("Scheduling ${name}...")

        val schedRecording = Recording(name, startDate, endDate)
        schedRecording.schedule(applicationContext)

        doAsync {
            db = initialiseDb(applicationContext)
            db.recordingDao().insert(schedRecording)
            db.close()

            uiThread {
                recordings.add(schedRecording)
                toast("Scheduled ${name}.")
                list.adapter?.notifyItemInserted(recordings.size - 1)
                list.visibility = View.VISIBLE
                //todo empty_view.visibility = View.GONE
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun openScheduleRecordingDialog(initialStartDate: Calendar, initialEndDate: Calendar) {
        val builder = AlertDialog.Builder(this)
        view = layoutInflater.inflate(R.layout.schedule_recording_dialog, null)
        if (usesCustomFilename(this)) view!!.name.visibility = View.VISIBLE
        else view!!.name.visibility = View.GONE

        startDate = initialStartDate
        endDate = initialEndDate
        setStartDatetime(startDate)
        setEndDatetime(endDate)

        builder.run {
            setView(view)
            setTitle(getString(R.string.schedule_recording))
            setPositiveButton(getString(R.string.schedule), null) // will be overridden
            setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        }

        // set up date and time buttons
        view?.run {
            startDateButton?.onClick {
                showDatePicker(this@SchedRecordingsActivity, startDate, ::setStartDatetime)
            }
            endDateButton?.onClick {
                showDatePicker(this@SchedRecordingsActivity, endDate, ::setEndDatetime)
            }
            startTimeButton?.onClick {
                showTimePicker(this@SchedRecordingsActivity, startDate, ::setStartDatetime)
            }
            endTimeButton?.onClick {
                showTimePicker(this@SchedRecordingsActivity, endDate, ::setEndDatetime)
            }
        }

        val dialog = builder.create()

        // Override positive button, so that it only dismisses if validation passes
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val name = if (usesCustomFilename(this)) {
                    val inputName = view?.name?.input?.text?.toString()
                    if (inputName == null || inputName == "") "(no title)"
                    else inputName
                } else getNameForRecording(this, startDate.time)!!

                val existingRecordings = getIntersectingRecordings(recordings, startDate, endDate)

                if (!hasValidDate()) {
                    AlertDialog.Builder(this).create().apply {
                        setMessage("Start time cannot be after the end time.")
                        setButton(AlertDialog.BUTTON_POSITIVE, "OK") { subDialog, _ ->
                            subDialog.dismiss()
                        }
                        show()
                    }

                } else if (existingRecordings.isNotEmpty()) {
                    var message = "There are already ${existingRecordings.size} recordings " +
                            "scheduled between those two times. Specifically: <ul style=\"list-style:none;\">"
                    existingRecordings.forEach { message += "<li>${it.toHtml()}</li>" }
                    message += "</ul>"

                    AlertDialog.Builder(this).create().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT))
                        } else {
                            @Suppress("DEPRECATION")
                            setMessage(Html.fromHtml(message))
                        }
                        setButton(AlertDialog.BUTTON_POSITIVE, "OK") { subDialog, _ ->
                            subDialog.dismiss()
                        }
                        show()
                    }

                } else if (recordingNameExists(name)) { // already a file with that name
                    val replacementName = generateUniqueName(name)
                    if (usesCustomFilename(this)) {
                        AlertDialog.Builder(this).create().apply {
                            setTitle("There is already a recording with that name")
                            setMessage("Do you want to save it as $replacementName instead?")
                            setButton(AlertDialog.BUTTON_POSITIVE, "Yes") { subDialog, _ ->
                                subDialog.dismiss()
                                dialog.dismiss()
                                scheduleRecording(replacementName)
                            }
                            setButton(AlertDialog.BUTTON_NEGATIVE, "No") { subDialog, _ ->
                                subDialog.dismiss()
                            }
                            show()
                        }
                    } else { // user doesn't specify name, so just use the replacement name
                        dialog.dismiss()
                        scheduleRecording(replacementName)
                    }

                } else {
                    scheduleRecording(name)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()

    }

    private fun recordingNameExists(name: String): Boolean {
        recordings.forEach { if (it.name == name) return true }
        return false
    }

    /**
     * Opens a date picker initialised to date.
     * It then calls setDateCallback with the date the user picked.
     */
    private fun showDatePicker(
        context: Context,
        date: Calendar,
        setDateCallback: (Calendar) -> Unit
    ) {
        DatePickerDialog(
            context,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                date.apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthOfYear)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                setDateCallback(date)
            },
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH),
            date.get(Calendar.DATE)
        ).run {
            show()
        }
    }

    /**
     * Opens a time picker initialised to date.
     * It then calls setTimeCallback with the time the user picked.
     */
    private fun showTimePicker(
        context: Context,
        date: Calendar,
        setTimeCallback: (Calendar) -> Unit
    ) {
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

    private fun setStartDatetime(date: Calendar) {
        view?.startDateButton?.run { setDate(date, this) }
        view?.startTimeButton?.run { setTime(date, this) }
        startDate = date
        validate()
    }

    private fun setEndDatetime(date: Calendar) {
        view?.endDateButton?.run { setDate(date, this) }
        view?.endTimeButton?.run { setTime(date, this) }
        endDate = date
        validate()
    }

    private fun hasValidDate() = startDate < endDate

    private fun validate() {
        if (!hasValidDate()) {
            view?.startDateButton?.textColor = Util.invalidColour
            view?.startTimeButton?.textColor = Util.invalidColour
        } else {
            view?.startDateButton?.textColor = Util.validColour
            view?.startTimeButton?.textColor = Util.validColour
        }
    }

    private fun generateUniqueName(name: String): String {
        var filename = name
        var i = 2
        while (recordingNameExists(filename)) {
            filename = "$name ($i)"
            i++
        }
        return filename
    }
}