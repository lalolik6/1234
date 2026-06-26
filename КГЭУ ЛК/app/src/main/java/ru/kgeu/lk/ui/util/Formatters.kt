package ru.kgeu.lk.ui.util

import ru.kgeu.lk.data.model.ScheduleLesson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun ScheduleLesson.timeRange(): String {
    val startText = parseDateTime(startDateTime)?.format(timeFormatter)
        ?: start?.take(5)
        ?: "--:--"
    val endText = parseDateTime(endDateTime)?.format(timeFormatter)
        ?: end?.take(5)
        ?: "--:--"
    return "$startText – $endText"
}

private fun parseDateTime(raw: String?): LocalDateTime? {
    if (raw.isNullOrBlank()) return null
    return runCatching { LocalDateTime.parse(raw.substring(0, minOf(19, raw.length))) }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        }.getOrNull()
}

fun semesterTitle(sem: Int?): String = when (sem) {
    1 -> "Осень"
    2 -> "Весна"
    else -> sem?.toString() ?: "—"
}
