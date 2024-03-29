package nz.co.olliechick.hivo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_sched_recordings.*
import kotlinx.android.synthetic.main.schedule_recording_dialog.view.*
import nz.co.olliechick.hivo.util.Constants.Companion.invalidColour
import nz.co.olliechick.hivo.util.Constants.Companion.validColour
import nz.co.olliechick.hivo.util.Database.Companion.initialiseDb
import nz.co.olliechick.hivo.util.Preferences.Companion.getMaximumRecordTime
import nz.co.olliechick.hivo.util.Recordings.Companion.getOverlappingRecordings
import nz.co.olliechick.hivo.util.StringProcessing
import nz.co.olliechick.hivo.util.StringProcessing.Companion.formatDateRange
import nz.co.olliechick.hivo.util.StringProcessing.Companion.getNameForRecording
import nz.co.olliechick.hivo.util.StringProcessing.Companion.usesCustomFilename
import nz.co.olliechick.hivo.util.Ui.Companion.showDatePicker
import nz.co.olliechick.hivo.util.Ui.Companion.showTimePicker
import nz.co.olliechick.hivo.util.Ui.Companion.toast
import nz.co.olliechick.hivo.util.getTimeInMinutes
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onClick
import org.jetbrains.anko.textColor
import org.jetbrains.anko.uiThread
import java.text.SimpleDateFormat
import java.util.*


class SchedRecordingsActivity : AppCompatActivity() {

    private var view: View? = null
    private lateinit var db: RecordingDatabase

    // start and end date initialised to the epoch to easily spot bugs, as these should be
    // set to whatever [openScheduleRecordingDialog] is called with before being displayed.
    private var startDate: Calendar = GregorianCalendar(1970, 0, 1)
    private var prevStartDate: Calendar = GregorianCalendar(1970, 0, 1)
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
        setUpItemTouchHelper()

        fab.onClick {
            //Start at next full hour eg (2pm, 2:59:59pm) -> 3pm, and finish [max record time] later
            val start = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 60)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).also {
                it.add(Calendar.MINUTE, getMaximumRecordTime(this))
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
            val allRecordings = db.recordingDao().getSchedRecordings()
            uiThread {
                recordings = ArrayList(allRecordings)
                if (recordings.size == 0) {
                    list.visibility = View.GONE
                    //todo empty_view.visibility = View.VISIBLE
                }
            }
            db.close()
        }
    }

    /**
     * Adds "swipe to delete" feature. Adapted from
     * https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete
     */
    private fun setUpItemTouchHelper() {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                // we want to cache these and not allocate anything repeatedly in the onChildDraw method
                lateinit var background: Drawable
                lateinit var xMark: Drawable
                var xMarkMargin: Int = 0
                var initiated: Boolean = false

                private fun init() {
                    background = ColorDrawable(Color.RED)
                    xMark = ContextCompat.getDrawable(
                        this@SchedRecordingsActivity,
                        R.drawable.ic_clear_24dp
                    )!!
                    xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                    xMarkMargin =
                        this@SchedRecordingsActivity.resources
                            .getDimension(R.dimen.ic_clear_margin).toInt()
                    initiated = true
                }

                // not important, we don't want drag & drop
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val position = viewHolder.adapterPosition
                    val adapter = recyclerView.adapter as SchedRecordingAdapter
                    return if (adapter.isPendingRemoval(position)) 0
                    else super.getSwipeDirs(recyclerView, viewHolder)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    val swipedPosition = viewHolder.adapterPosition
                    val adapter = list.adapter as SchedRecordingAdapter
                    adapter.pendingRemoval(swipedPosition, applicationContext)
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val itemView = viewHolder.itemView

                    // not sure why, but this method get's called for viewholder that are already swiped away
                    if (viewHolder.adapterPosition == -1) return

                    if (!initiated) init()

                    // draw red background
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    // draw x mark
                    val itemHeight = itemView.getBottom() - itemView.getTop()
                    val intrinsicWidth = xMark.intrinsicWidth
                    val intrinsicHeight = xMark.intrinsicWidth

                    val xMarkLeft = itemView.right - xMarkMargin - intrinsicWidth
                    val xMarkRight = itemView.right - xMarkMargin
                    val xMarkTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val xMarkBottom = xMarkTop + intrinsicHeight
                    xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom)

                    xMark.draw(c)

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }

            }
        val mItemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        mItemTouchHelper.attachToRecyclerView(list)
    }

    private fun scheduleRecording(name: String) {
        toast(getString(R.string.scheduling, name))

        doAsync {
            val schedRecording = Recording(name, startDate, endDate)

            db = initialiseDb(applicationContext)
            schedRecording.id = db.recordingDao().insert(schedRecording)
            db.close()

            schedRecording.schedule(applicationContext)
            recordings.add(schedRecording)

            uiThread {
                toast(getString(R.string.scheduled, name))

                list.adapter?.notifyItemInserted(recordings.size - 1)
                list.visibility = View.VISIBLE
                //todo empty_view.visibility = View.GONE
            }
        }
    }

    private fun getRecordingNameFromViewOrDefault(): String {
        return if (usesCustomFilename(this)) {
            val inputName = view?.name?.input?.text?.toString()
            if (inputName == null || inputName == "") getString(R.string.no_title)
            else inputName
        } else getNameForRecording(this, startDate.time)!!
    }

    @SuppressLint("InflateParams")
    private fun openScheduleRecordingDialog(initialStartDate: Calendar, initialEndDate: Calendar) {
        val builder = AlertDialog.Builder(this)
        view = layoutInflater.inflate(R.layout.schedule_recording_dialog, null)
        if (usesCustomFilename(this)) view!!.name.visibility = View.VISIBLE
        else view!!.name.visibility = View.GONE

        startDate = initialStartDate
        prevStartDate = initialStartDate
        endDate = initialEndDate
        updateStartDatetimeLabels()
        updateEndDatetimeLabels()

        builder.run {
            setView(view)
            setTitle(getString(R.string.schedule_recording))
            setPositiveButton(getString(R.string.schedule), null) // will be overridden
            setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        }

        // set up date and time buttons
        view?.run {
            startDateButton?.onClick {
                showDatePicker(this@SchedRecordingsActivity, startDate, ::updateStartDatetimeLabels)
            }
            endDateButton?.onClick {
                showDatePicker(this@SchedRecordingsActivity, endDate, ::updateEndDatetimeLabels)
            }
            startTimeButton?.onClick {
                showTimePicker(this@SchedRecordingsActivity, startDate, ::updateStartDatetimeLabels)
            }
            endTimeButton?.onClick {
                showTimePicker(this@SchedRecordingsActivity, endDate, ::updateEndDatetimeLabels)
            }
        }

        val dialog = builder.create()

        // Override positive button, so that it only dismisses if validation passes
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val name = getRecordingNameFromViewOrDefault()
                val overlappingRecordings = getOverlappingRecordings(recordings, startDate, endDate)
                val validationDialogBuilder = AlertDialog.Builder(this)
                val usesCustomName = usesCustomFilename(this)

                // Validation
                doAsync {

                    val nameExists = db.recordingDao().nameExists(name)
                    val replacementName = if (nameExists) generateUniqueName(name) else ""
                    db.close()

                    uiThread {
                        if (!startsBeforeItEnds()) {
                            // Check start date is before end date
                            validationDialogBuilder.apply {
                                setMessage(getString(R.string.start_time_not_after_end_time))
                                setPositiveButton(getString(R.string.ok)) { subDialog, _ -> subDialog.dismiss() }

                                create()
                                show()

                            }

                        } else if (overlappingRecordings.isNotEmpty()) {
                            // Check that there are no recordings scheduled for that time
                            val nRecordings = overlappingRecordings.size
                            var message = resources.getQuantityString(
                                R.plurals.already_n_recordings_scheduled, nRecordings, nRecordings,
                                formatDateRange(startDate, endDate)
                            ) + "<ul style =\"list-style:none;\">"
                            overlappingRecordings.forEach { message += "<li>${it.toHtml()}</li>" }
                            message += "</ul>"

                            validationDialogBuilder.apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT))
                                } else {
                                    @Suppress("DEPRECATION")
                                    setMessage(Html.fromHtml(message))
                                }
                                setPositiveButton(getString(R.string.ok)) { subDialog, _ -> subDialog.dismiss() }

                                create()
                                show()

                            }

                        } else if (!startsInTheFuture()) {
                            // Check that it starts in the future
                            validationDialogBuilder.apply {
                                setMessage(getString(R.string.recording_must_start_in_future))
                                setPositiveButton(getString(R.string.ok)) { subDialog, _ ->
                                    subDialog.dismiss()
                                    updateValidationLabels()
                                }

                                create()
                                show()

                            }

                        } else if (endDate.getTimeInMinutes() - startDate.getTimeInMinutes() > 6 * 60) {
                            // Passes if clip is shorter than 6 hours
                            validationDialogBuilder.apply {
                                setMessage(getString(R.string.recordings_must_be_less_than_6_hrs))
                                setPositiveButton(getString(R.string.ok)) { subDialog, _ ->
                                    subDialog.dismiss()
                                }
                                create()
                                show()
                            }
                        } else if (nameExists) {
                            // Check that there is no file with that name
                            if (usesCustomName) {
                                validationDialogBuilder.apply {
                                    setTitle(getString(R.string.already_recording_with_name))
                                    setMessage(getString(R.string.save_as_instead, replacementName))
                                    setPositiveButton(getString(R.string.yes)) { subDialog, _ ->
                                        subDialog.dismiss()
                                        dialog.dismiss()
                                        scheduleRecording(replacementName)
                                    }
                                    setNegativeButton(getString(R.string.no)) { subDialog, _ -> subDialog.dismiss() }

                                    create()
                                    show()

                                }
                            } else { // user doesn't specify name, so just use the replacement name
                                dialog.dismiss()
                                scheduleRecording(replacementName)
                            }

                        } else {
                            // All valid :)
                            scheduleRecording(name)
                            dialog.dismiss()
                        }
                    }
                }
            }

        }


        dialog.show()
    }

    private fun setDate(date: Calendar, dateButton: Button) {
        val format = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
        dateButton.text = format.format(date.time)
    }

    private fun setTime(time: Calendar, timeButton: Button) {
        val format = SimpleDateFormat("h:mm aa", Locale.ENGLISH)
        timeButton.text = format.format(time.time)
    }

    private fun updateStartDatetimeLabels() {
        // Update end datetime to reflect update in start datetime - the end datetime will
        // increase/decrease as start does, to maintain the duration
        // E.g. if start = 6pm and end = 7pm, if start is updated to 6:05pm, end will be 7:05pm
        // This is what Google Calendar does when you create an event.
        endDate.add(
            Calendar.MINUTE,
            ((startDate.timeInMillis - prevStartDate.timeInMillis) / (1000 * 60)).toInt()
        )
        updateEndDatetimeLabels()

        view?.startDateButton?.run { setDate(startDate, this) }
        view?.startTimeButton?.run { setTime(startDate, this) }
        prevStartDate = startDate.clone() as Calendar
        updateValidationLabels()
    }

    private fun updateEndDatetimeLabels() {
        view?.endDateButton?.run { setDate(endDate, this) }
        view?.endTimeButton?.run { setTime(endDate, this) }
        updateValidationLabels()
    }

    private fun startsInTheFuture() = Date() < startDate.time
    private fun startsBeforeItEnds() = startDate < endDate

    private fun hasValidDate() = startsBeforeItEnds() && startsInTheFuture()

    private fun updateValidationLabels() {
        if (hasValidDate()) {
            view?.startDateButton?.textColor = validColour
            view?.startTimeButton?.textColor = validColour
        } else {
            view?.startDateButton?.textColor = invalidColour
            view?.startTimeButton?.textColor = invalidColour
        }
    }

    private fun generateUniqueName(name: String): String =
        StringProcessing.generateUniqueName(name) { altName -> db.recordingDao().nameExists(altName) }

}