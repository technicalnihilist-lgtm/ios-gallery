package com.hubclub.iosgallery.util

import com.hubclub.iosgallery.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DateGroup(val label: String, val items: List<MediaItem>)

private val dayFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
private val yearFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

/** Groups items into Today / Yesterday / "12 March 2025" sections. */
fun groupByDate(items: List<MediaItem>): List<DateGroup> {
    if (items.isEmpty()) return emptyList()
    val today = startOfDay(System.currentTimeMillis())
    val yesterday = today - TimeUnit.DAYS.toMillis(1)
    val groups = LinkedHashMap<String, MutableList<MediaItem>>()
    for (item in items) {
        val day = startOfDay(item.dateMillis)
        val label = when {
            day == today -> "Today"
            day == yesterday -> "Yesterday"
            else -> yearFormat.format(Date(item.dateMillis))
        }
        groups.getOrPut(label) { mutableListOf() }.add(item)
    }
    return groups.map { (label, list) -> DateGroup(label, list) }
}

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun formatDate(millis: Long): String = dayFormat.format(Date(millis))

fun formatDateTime(millis: Long): String =
    "${dayFormat.format(Date(millis))} at ${timeFormat.format(Date(millis))}"

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%d:%02d", minutes, seconds)
}

fun formatCount(count: Int, singular: String, plural: String = singular + "s"): String =
    if (count == 1) "1 $singular" else "$count $plural"

fun formatResolution(width: Int, height: Int): String =
    if (width > 0 && height > 0) "$width × $height" else "—"

/** 12,345 -> "12,345" (Indian-friendly grouping handled by Locale default). */
fun formatNumber(value: Int): String = String.format(Locale.getDefault(), "%,d", value)
