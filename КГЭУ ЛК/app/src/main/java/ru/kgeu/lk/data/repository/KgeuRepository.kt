package ru.kgeu.lk.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import ru.kgeu.lk.data.parser.VedListParser
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
                lessonStart.startsWith(targetIso)
        }
        val lessons = filteredByDate
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
        val semesters = runCatching {
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

        // API `IntermediatePerfomance` возвращает только закрытые предметы.
        // Добавляем недостающие (в т.ч. незакрытые) из списка ведомостей Ved/.
        val mergedSemesters = mergeWithVedList(semesters)

        // Подтягиваем «Итоговый рейтинг по КТ» из ведомости каждого предмета,
        // чтобы показывать балл на карточке без захода в предмет.
        coroutineScope {
            mergedSemesters.map { semester ->
                async {
                    val disciplines = semester.disciplines.map { discipline ->
                        async { discipline.withKtRating() }
                    }.awaitAll()
                    semester.copy(disciplines = disciplines)
                }
            }.awaitAll()
        }
    }

    /**
     * Дополняет список семестров предметами из страницы Ved/ (там есть все
     * предметы текущего семестра, в т.ч. незакрытые). Предметы сопоставляются
     * по ratingID; недостающие добавляются в семестр с наибольшим пересечением.
     */
    private suspend fun mergeWithVedList(semesters: List<SemesterRating>): List<SemesterRating> {
        val vedItems = runCatching {
            fetchVedListHtml()?.let { VedListParser.parse(it) }
        }.getOrNull().orEmpty()
        if (vedItems.isEmpty()) return semesters

        val knownIds = semesters.flatMap { it.disciplines.mapNotNull { d -> d.ratingID } }.toSet()
        val missing = vedItems.filter { it.ratingID !in knownIds }
        if (missing.isEmpty()) return semesters

        val newDisciplines = missing.map { item ->
            DisciplineGrade(
                name = item.name,
                controlForm = item.controlForm,
                ratingID = item.ratingID,
                closed = item.closed,
            )
        }

        // Семестр, к которому относится список Ved/ — тот, где больше всего
        // совпадений по ratingID с уже известными предметами.
        val vedIds = vedItems.map { it.ratingID }.toSet()
        val targetIndex = semesters.indices.maxByOrNull { index ->
            semesters[index].disciplines.count { it.ratingID in vedIds }
        }?.takeIf { index ->
            semesters[index].disciplines.any { it.ratingID in vedIds }
        }

        if (targetIndex == null) {
            // Не нашли подходящий семестр — показываем список как отдельный семестр.
            return semesters + SemesterRating(disciplines = newDisciplines)
        }

        return semesters.mapIndexed { index, semester ->
            if (index == targetIndex) {
                semester.copy(disciplines = semester.disciplines + newDisciplines)
            } else {
                semester
            }
        }
    }

    private suspend fun DisciplineGrade.withKtRating(): DisciplineGrade {
        val ratingId = ratingID ?: return this
        val rating = runCatching {
            fetchVedHtml(ratingId)?.let { VedParser.parseRatingKt(it) }
        }.getOrNull()
        return if (rating.isNullOrBlank()) this else copy(ktRatingKt = rating)
    }

    suspend fun loadGradeDetails(discipline: DisciplineGrade): List<GradePoint> = withContext(Dispatchers.IO) {
        val ratingId = discipline.ratingID ?: return@withContext emptyList()
        val html = fetchVedHtml(ratingId)
            ?: throw IllegalStateException("Не удалось загрузить ведомость")
        val points = VedParser.parse(html)
        if (points.isEmpty()) {
            throw IllegalStateException("Детальные баллы не найдены в ведомости")
        }
        points
    }

    private suspend fun fetchVedHtml(ratingId: Int): String? = withContext(Dispatchers.IO) {
        val token = sessionStorage.getToken() ?: return@withContext null
        val urls = listOf(
            "https://kabinet.kgeu.ru/Ved/Ved.aspx?id=$ratingId",
        )
        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .build()
            val html = runCatching {
                apiClient.httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull()
            if (!html.isNullOrBlank()) return@withContext html
        }
        null
    }

    private suspend fun fetchVedListHtml(): String? = withContext(Dispatchers.IO) {
        val token = sessionStorage.getToken() ?: return@withContext null
        val request = Request.Builder()
            .url("https://kabinet.kgeu.ru/Ved/")
            .header("Authorization", "Bearer $token")
            .build()
        runCatching {
            apiClient.httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
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
