package ru.kgeu.lk.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kgeu.lk.data.model.DisciplineGrade
import ru.kgeu.lk.data.model.GradePoint
import ru.kgeu.lk.data.model.SemesterRating
import ru.kgeu.lk.data.model.UiState
import ru.kgeu.lk.ui.util.displayName
import ru.kgeu.lk.ui.util.ktRating
import ru.kgeu.lk.ui.util.semesterTitle

@Composable
fun GradesScreen(
    state: UiState<List<SemesterRating>>,
    selectedDiscipline: DisciplineGrade?,
    detailsState: UiState<List<GradePoint>>,
    onRefresh: () -> Unit,
    onOpenDiscipline: (DisciplineGrade) -> Unit,
    onCloseDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedDiscipline != null) {
        GradeDetailsScreen(
            discipline = selectedDiscipline,
            state = detailsState,
            onBack = onCloseDetails,
            onRetry = { onOpenDiscipline(selectedDiscipline) },
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Ведомости успеваемости",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        when {
            state.loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> ErrorBlock(message = state.error, onRetry = onRefresh)

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.data.orEmpty().forEach { semester ->
                        item(key = "sem-${semester.year}-${semester.sem}") {
                            Text(
                                text = "${semesterTitle(semester.sem)} ${semester.year.orEmpty()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        itemsIndexed(
                            items = semester.disciplines,
                            key = { index, discipline ->
                                "grade-${semester.year}-${semester.sem}-$index-${discipline.ratingID}"
                            },
                        ) { _, discipline ->
                            DisciplineCard(discipline, onOpenDiscipline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisciplineCard(
    discipline: DisciplineGrade,
    onOpen: (DisciplineGrade) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(discipline) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = discipline.displayName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = discipline.controlForm ?: discipline.type.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Итоговый рейтинг КТ: ${discipline.ktRating() ?: "—"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            discipline.teacher?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeDetailsScreen(
    discipline: DisciplineGrade,
    state: UiState<List<GradePoint>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(discipline.displayName(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when {
                state.loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> ErrorBlock(message = state.error, onRetry = onRetry)

                state.data.isNullOrEmpty() -> {
                    Text("Детальные баллы не найдены в ведомости.")
                }

                else -> {
                    val points = state.data.orEmpty()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            items = points,
                            key = { index, point -> "point-$index-${point.title}" },
                        ) { _, point ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(point.title, modifier = Modifier.weight(1f))
                                    Text(
                                        point.value,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
