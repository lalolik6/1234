package ru.kgeu.lk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kgeu.lk.data.model.DisciplineGrade
import ru.kgeu.lk.data.model.GradePoint
import ru.kgeu.lk.data.model.SemesterRating
import ru.kgeu.lk.data.model.UiState
import ru.kgeu.lk.data.model.UserProfile
import ru.kgeu.lk.data.repository.KgeuRepository
import ru.kgeu.lk.data.repository.ScheduleResult
import java.time.LocalDate

class AppViewModel(
    private val repository: KgeuRepository,
) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _user = MutableStateFlow<UserProfile?>(null)
    val user = _user.asStateFlow()

    private val _loginState = MutableStateFlow(UiState<Unit>())
    val loginState = _loginState.asStateFlow()

    private val _scheduleState = MutableStateFlow(UiState<ScheduleResult>())
    val scheduleState = _scheduleState.asStateFlow()

    private val _gradesState = MutableStateFlow(UiState<List<SemesterRating>>())
    val gradesState = _gradesState.asStateFlow()

    private val _gradeDetailsState = MutableStateFlow(UiState<List<GradePoint>>())
    val gradeDetailsState = _gradeDetailsState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedDiscipline = MutableStateFlow<DisciplineGrade?>(null)
    val selectedDiscipline = _selectedDiscipline.asStateFlow()

    init {
        viewModelScope.launch {
            val session = repository.restoreSession()
            if (session != null) {
                _user.value = session.user
                _isLoggedIn.value = true
                refreshAll()
            }
        }
    }

    fun login(login: String, password: String) {
        viewModelScope.launch {
            _loginState.update { it.copy(loading = true, error = null) }
            runCatching { repository.login(login, password) }
                .onSuccess { session ->
                    _user.value = session.user
                    _isLoggedIn.value = true
                    _loginState.update { UiState() }
                    _scheduleState.value = UiState(loading = true)
                    _gradesState.value = UiState(loading = true)
                    loadSchedule(_selectedDate.value)
                    loadGrades()
                }
                .onFailure { error ->
                    _loginState.update {
                        UiState(error = error.message ?: "Ошибка входа")
                    }
                }
        }
    }

    fun logout() {
        repository.logout()
        _isLoggedIn.value = false
        _user.value = null
        _scheduleState.value = UiState()
        _gradesState.value = UiState()
        _gradeDetailsState.value = UiState()
        _selectedDiscipline.value = null
    }

    fun refreshAll() {
        loadSchedule(_selectedDate.value)
        loadGrades()
    }

    fun loadSchedule(date: LocalDate) {
        _selectedDate.value = date
        viewModelScope.launch {
            _scheduleState.update { it.copy(loading = true, error = null) }
            runCatching { repository.loadSchedule(date) }
                .onSuccess { result ->
                    _scheduleState.update { UiState(data = result) }
                }
                .onFailure { error ->
                    _scheduleState.update {
                        UiState(error = error.message ?: "Не удалось загрузить расписание")
                    }
                }
        }
    }

    fun shiftScheduleDays(delta: Long) {
        loadSchedule(_selectedDate.value.plusDays(delta))
    }

    fun loadGrades() {
        viewModelScope.launch {
            _gradesState.update { it.copy(loading = true, error = null) }
            runCatching { repository.loadGrades() }
                .onSuccess { grades ->
                    _gradesState.update { UiState(data = grades) }
                }
                .onFailure { error ->
                    _gradesState.update {
                        UiState(error = error.message ?: "Не удалось загрузить баллы")
                    }
                }
        }
    }

    fun openDiscipline(discipline: DisciplineGrade) {
        _selectedDiscipline.value = discipline
        viewModelScope.launch {
            _gradeDetailsState.update { it.copy(loading = true, error = null) }
            runCatching { repository.loadGradeDetails(discipline) }
                .onSuccess { points ->
                    _gradeDetailsState.update { UiState(data = points) }
                }
                .onFailure { error ->
                    _gradeDetailsState.update {
                        UiState(error = error.message ?: "Не удалось загрузить детали")
                    }
                }
        }
    }

    fun closeDisciplineDetails() {
        _selectedDiscipline.value = null
        _gradeDetailsState.value = UiState()
    }
}

class AppViewModelFactory(
    private val repository: KgeuRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(repository) as T
    }
}
