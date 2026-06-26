package ru.kgeu.lk.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import ru.kgeu.lk.data.api.KgeuApiClient
import ru.kgeu.lk.data.model.DisciplineGrade
import ru.kgeu.lk.data.model.GradePoint
import ru.kgeu.lk.data.model.LoginRequest
import ru.kgeu.lk.data.model.ScheduleLesson
import ru.kgeu.lk.data.model.SemesterRating
import ru.kgeu.lk.data.model.SessionState
import ru.kgeu.lk.data.model.UserProfile
import ru.kgeu.lk.data.parser.GradesJsonParser
import ru.kgeu.lk.data.parser.ScheduleJsonParser
import ru.kgeu.lk.data.parser.VedParser
import ru.kgeu.lk.data.storage.SessionStorage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class KgeuRepository(
    private val apiClient: KgeuApiClient,
    private val sessionStorage: SessionStorage,
) {
    private val api get() = apiClient.api
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    suspend fun restoreSession(): SessionState? {
        val token = sessionStorage.getToken() ?: return null
        return try {
            val user = loadUserProfile()
            SessionState(token = token, login = sessionStorage.getLogin(), user = user)
        } catch (_: Exception) {
            logout()
            null
        }
    }

    suspend fun login(login: String, password: String): SessionState = withContext(Dispatchers.IO) {
        val fingerprint = sessionStorage.getFingerprint()
        val response = api.login(
            LoginRequest(
                userName = login.trim(),
                password = password,
                fingerprint = fingerprint,
            ),
        )

        if (response.state == -1 || response.state == -2) {
            throw IllegalStateException(response.msg ?: "Ошибка авторизации")
        }

        val payload = response.data
        if (payload?.require2FA == true) {
            throw IllegalStateException("Нужен код двухфакторной аутентификации. Пока отключите 2FA в личном кабинете на сайте.")
        }

        val token = payload?.accessToken
            ?: throw IllegalStateException("Сервер не вернул токен")

        sessionStorage.saveToken(token)
        sessionStorage.saveLogin(login.trim())
        sessionStorage.savePassword(password)

        val user = loadUserProfile()
        sessionStorage.saveUser(user)
        user.groupID?.let { sessionStorage.saveGroupId(it) }

        SessionState(token = token, login = login.trim(), user = user)
    }

    suspend fun loadUserProfile(): UserProfile = withContext(Dispatchers.IO) {
        val response = api.getUserInfo()
        if (response.state != 1) {
            throw IllegalStateException(response.msg ?: "Не удалось получить профиль")
        }
        response.data?.user ?: throw IllegalStateException("Пустой профиль пользователя")
    }

    suspend fun loadSchedule(date: LocalDate = LocalDate.now()): ScheduleResult = withContext(Dispatchers.IO) {
        val groupId = resolveGroupId()
        val years = api.listYears().data?.years.orEmpty()
        val year = pickStudyYear(years, date)
        val envelope = api.schedule(
            groupId = groupId,
            date = date.format(displayDateFormatter),
            year = year,
        )
        if (envelope.state == 0) {
            throw IllegalStateException("Нет доступа к расписанию")
        }
        val data = envelope.data ?: throw IllegalStateException("Не удалось загрузить расписание")
        val parsedLessons = ScheduleJsonParser.parseLessons(data)
        val targetIso = date.toString()
        val targetDisplay = date.format(displayDateFormatter)
        val filteredByDate = parsedLessons.filter { lesson ->
            val lessonDate = lesson.date.orEmpty()
            val lessonStart = lesson.startDateTime.orEmpty()
            lessonDate.startsWith(targetIso) ||
                lessonDate.startsWith(targetDisplay) ||
                lessonStart.startsWith(targetIso) ||
                (lessonDate.isBlank() && lessonStart.isBlank())
        }
        val lessons = (if (filteredByDate.isNotEmpty()) filteredByDate else parsedLessons)
            .filter { lesson -> !lesson.cancelled.orFalse() }
            .filter { lesson -> lesson.hasVisibleContent() }
            .sortedBy { it.startDateTime ?: it.start ?: it.date.orEmpty() }

        val info = ScheduleJsonParser.parseInfo(data)
        ScheduleResult(
            groupName = info?.group?.name,
            academicYear = year,
            weekType = info?.typesWeek
                ?.firstOrNull { it.typeWeekID == info.curNumNed }
                ?.shortName,
            lessons = lessons,
        )
    }

    suspend fun loadGrades(): List<SemesterRating> = withContext(Dispatchers.IO) {
        runCatching {
            val envelope = api.grades(onlyDebts = false)
            val data = envelope.data ?: return@runCatching emptyList()
            GradesJsonParser.parseRating(data)
        }.getOrElse { emptyList() }
            .map { semester ->
                semester.copy(
                    disciplines = semester.disciplines.filter { it.hasVisibleContent() },
                )
            }
            .filter { it.disciplines.isNotEmpty() }
            .sortedByDescending { it.year + (it.sem ?: 0).toString() }
    }

    suspend fun loadGradeDetails(discipline: DisciplineGrade): List<GradePoint> = withContext(Dispatchers.IO) {
        val ratingId = discipline.ratingID ?: return@withContext emptyList()
        val token = sessionStorage.getToken() ?: return@withContext emptyList()

        val urls = listOf(
            "https://kabinet.kgeu.ru/Ved/Ved.aspx?id=$ratingId",
            "https://edu.donstu.ru/Ved/Ved.aspx?id=$ratingId",
        )

        var lastError: Exception? = null
        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build()

            runCatching {
                apiClient.httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Не удалось загрузить ведомость (${response.code})")
                    }
                    val html = response.body?.string().orEmpty()
                    val points = VedParser.parse(html)
                    if (points.isNotEmpty()) return@withContext points
                    throw IllegalStateException("Пустая ведомость")
                }
            }.onSuccess { return@withContext it }
                .onFailure { lastError = it as? Exception ?: Exception(it) }
        }

        throw lastError ?: IllegalStateException("Не удалось загрузить ведомость")
    }

    fun logout() {
        sessionStorage.clear()
    }

    private suspend fun resolveGroupId(): Int {
        sessionStorage.getGroupId()?.let { return it }

        val rasp = api.currentUserRasp().data
        val fromRasp = rasp?.id ?: rasp?.groupID
        if (fromRasp != null && fromRasp > 0) {
            sessionStorage.saveGroupId(fromRasp)
            return fromRasp
        }

        sessionStorage.getUser()?.groupID?.let { groupId ->
            sessionStorage.saveGroupId(groupId)
            return groupId
        }

        val profile = loadUserProfile()
        sessionStorage.saveUser(profile)
        profile.groupID?.let { groupId ->
            sessionStorage.saveGroupId(groupId)
            return groupId
        }

        throw IllegalStateException("Не удалось определить группу")
    }

    private fun pickStudyYear(years: List<String>, date: LocalDate): String {
        val preferred = currentStudyYear(date)
        if (years.contains(preferred)) return preferred
        return years.maxOrNull() ?: preferred
    }

    private fun currentStudyYear(date: LocalDate): String {
        val startYear = if (date.monthValue >= 9) date.year else date.year - 1
        return "$startYear-${startYear + 1}"
    }

    private fun Boolean?.orFalse() = this == true
}

private fun ScheduleLesson.hasVisibleContent(): Boolean =
    !discipline.isNullOrBlank() ||
        !teacher.isNullOrBlank() ||
        !room.isNullOrBlank() ||
        !start.isNullOrBlank() ||
        !startDateTime.isNullOrBlank()

private fun DisciplineGrade.hasVisibleContent(): Boolean =
    !name.isNullOrBlank() || ratingID != null

data class ScheduleResult(
    val groupName: String?,
    val academicYear: String,
    val weekType: String?,
    val lessons: List<ScheduleLesson>,
)
