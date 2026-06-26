package ru.kgeu.lk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kgeu.lk.data.model.ScheduleLesson
import ru.kgeu.lk.data.model.UiState
import ru.kgeu.lk.data.repository.ScheduleResult
import ru.kgeu.lk.ui.util.timeRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (EEEE)", Locale("ru"))

@Composable
fun ScheduleScreen(
    selectedDate: LocalDate,
    state: UiState<ScheduleResult>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий день")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                state.data?.let { schedule ->
                    Text(
                        text = buildString {
                            schedule.groupName?.let { append(it) }
                            append(" • ${schedule.academicYear}")
                            schedule.weekType?.let { append(" • $it нед.") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = onNextDay) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий день")
            }
        }

        when {
            state.loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                ErrorBlock(message = state.error, onRetry = onRefresh)
            }

            else -> {
                val lessons = state.data?.lessons.orEmpty()
                if (lessons.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("На этот день пар нет")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(
                            items = lessons,
                            key = { index, lesson -> "lesson-$index-${lessonKey(lesson)}" },
                        ) { _, lesson ->
                            ScheduleCard(lesson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(lesson: ScheduleLesson) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Text(
                    text = lesson.timeRange(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                lesson.lessonNumber?.let {
                    Text(
                        text = "$it-е занятие",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column {
                Text(
                    text = lesson.discipline.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                lesson.teacher?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
                lesson.room?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Аудитория: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun lessonKey(lesson: ScheduleLesson): String =
    listOfNotNull(lesson.date, lesson.discipline, lesson.startDateTime, lesson.start).joinToString("|")

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        androidx.compose.material3.TextButton(onClick = onRetry) {
            Text("Повторить")
        }
    }
}
