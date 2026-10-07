package com.hpg.goaltracker.data

import java.util.Calendar

/**
 * Weekday selection stored as a bitmask where the bit index equals
 * [Calendar.DAY_OF_WEEK] (1=Sunday … 7=Saturday). Shared by goal schedules
 * and reminder settings.
 */
object WeekMask {

    const val ALL_DAYS = 0b1111_1110 // bits 1..7

    val WEEKDAYS = (1 shl Calendar.MONDAY) or (1 shl Calendar.TUESDAY) or
        (1 shl Calendar.WEDNESDAY) or (1 shl Calendar.THURSDAY) or (1 shl Calendar.FRIDAY)

    val WEEKENDS = (1 shl Calendar.SATURDAY) or (1 shl Calendar.SUNDAY)

    fun isSelected(mask: Int, dayOfWeek: Int): Boolean =
        (mask and (1 shl dayOfWeek)) != 0

    fun toggle(mask: Int, dayOfWeek: Int): Int =
        mask xor (1 shl dayOfWeek)

    fun count(mask: Int): Int = (1..7).count { isSelected(mask, it) }

    fun isToday(mask: Int, now: Long = System.currentTimeMillis()): Boolean {
        val dow = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
        return isSelected(mask, dow)
    }
}
