package nz.co.olliechick.hivo.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.*
import org.jetbrains.anko.toast as ankoToast

class Ui {
    companion object {
        fun Context.toast(message: CharSequence) {
            //Log.i("HiVo toast", message.toString())
            ankoToast(message)
        }

        /**
         * Opens a date picker initialised to date.
         * It then calls setDateCallback with the date the user picked.
         */
        fun showDatePicker(
            context: Context,
            date: Calendar,
            setDateCallback: () -> Unit
        ) {
            DatePickerDialog(
                context,
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    date.apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, monthOfYear)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    setDateCallback()
                },
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DATE)
            ).run { show() }
        }

        /**
         * Opens a time picker initialised to date.
         * It then calls setTimeCallback with the time the user picked.
         */
        fun showTimePicker(
            context: Context,
            date: Calendar,
            setTimeCallback: () -> Unit
        ) {
            TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                date.apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                setTimeCallback()
            }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), false).show()
        }
    }
}