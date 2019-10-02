package nz.co.olliechick.hivo.util

import java.util.*

fun Boolean.toInt() = if (this) 1 else 0

fun Calendar.getTimeInMinutes(): Long = this.timeInMillis / (1000 * 60)