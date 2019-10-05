package nz.co.olliechick.hivo.util

import java.util.*

fun Boolean.toInt() = if (this) 1 else 0

fun Calendar.getTimeInMinutes(): Long = this.timeInMillis / (1000 * 60)

/** Sets seconds and milliseconds of Calendar object to those of the object passed in. */
fun Calendar.setSecondsAndMillis(other: Calendar) {
    this.set(Calendar.SECOND, other.get(Calendar.SECOND))
    this.set(Calendar.MILLISECOND, other.get(Calendar.MILLISECOND))
}

/** Sets seconds and milliseconds of Calendar object to 0. */
fun Calendar.zeroSecondsAndMillis() {
    this.set(Calendar.SECOND, 0)
    this.set(Calendar.MILLISECOND, 0)
}
