package ru.kgeu.lk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.kgeu.lk.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val user by viewModel.user.collectAsStateWithLifecycle()
    val scheduleState by viewModel.scheduleState.collectAsStateWithLifecycle()
    val gradesState by viewModel.gradesState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDiscipline by viewModel.selectedDiscipline.collectAsStateWithLifecycle()
    val gradeDetailsState by viewModel.gradeDetailsState.collectAsStateWithLifecycle()

    // Автообновление при каждом выходе приложения на передний план
    // (открытие и возврат из свёрнутого состояния).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.refreshAll()
    }

    val refreshing = scheduleState.loading || gradesState.loading

    Scaffold(
        topBar = {
            if (selectedDiscipline == null) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = user?.fullName ?: user?.fio ?: "КГЭУ ЛК",
                                maxLines = 1,
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        actions = {
                            IconButton(onClick = viewModel::refreshAll) {
                                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                            }
                            IconButton(onClick = viewModel::logout) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выйти")
                            }
                        },
                    )
                    AnimatedVisibility(visible = refreshing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        bottomBar = {
            if (selectedDiscipline == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        label = { Text("Расписание") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Баллы") },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            0 -> ScheduleScreen(
                modifier = Modifier.padding(padding),
                selectedDate = selectedDate,
                state = scheduleState,
                onPreviousDay = { viewModel.shiftScheduleDays(-1) },
                onNextDay = { viewModel.shiftScheduleDays(1) },
                onRefresh = { viewModel.loadSchedule(selectedDate) },
            )

            else -> GradesScreen(
                modifier = Modifier.padding(padding),
                state = gradesState,
                selectedDiscipline = selectedDiscipline,
                detailsState = gradeDetailsState,
                onRefresh = viewModel::loadGrades,
                onOpenDiscipline = viewModel::openDiscipline,
                onCloseDetails = viewModel::closeDisciplineDetails,
            )
        }
    }
}
