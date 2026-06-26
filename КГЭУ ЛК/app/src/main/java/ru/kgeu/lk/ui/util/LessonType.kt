package ru.kgeu.lk.ui.util

import androidx.compose.ui.graphics.Color
import ru.kgeu.lk.data.model.ScheduleLesson

/**
 * Тип занятия определяется по префиксу в названии дисциплины
 * (например «экз Программная инженерия», «пр. Инжиниринг данных»).
 * Цвета: экзамен — красный, консультация — синий, практика/лаборатория —
 * жёлтый, лекция — зелёный.
 */
enum class LessonType(val label: String, val color: Color) {
    EXAM("Экзамен", Color(0xFFE53935)),
    CONSULT("Консультация", Color(0xFF1E88E5)),
    PRACTICE("Практика", Color(0xFFF5A623)),
    LAB("Лабораторная", Color(0xFFF5A623)),
    LECTURE("Лекция", Color(0xFF2E7D32)),
    OTHER("Занятие", Color(0xFF607D8B)),
}

private val typePrefixes: List<Pair<String, LessonType>> = listOf(
    "экз" to LessonType.EXAM,
    "конс" to LessonType.CONSULT,
    "лаб" to LessonType.LAB,
    "лек" to LessonType.LECTURE,
    "сем" to LessonType.PRACTICE,
    "пр" to LessonType.PRACTICE,
)

private fun firstToken(discipline: String?): String =
    discipline?.trim()?.substringBefore(' ')?.trimEnd('.', ',')?.lowercase().orEmpty()

fun ScheduleLesson.lessonType(): LessonType {
    val token = firstToken(discipline)
    return typePrefixes.firstOrNull { token.startsWith(it.first) }?.second ?: LessonType.OTHER
}

/** Название дисциплины без префикса типа занятия. */
fun ScheduleLesson.cleanDiscipline(): String {
    val text = discipline?.trim().orEmpty()
    if (lessonType() == LessonType.OTHER) return text
    val rest = text.substringAfter(' ', "").trim()
    return rest.ifBlank { text }
}
