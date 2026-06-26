package ru.kgeu.lk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class ApiEnvelope<T>(
    val state: Int? = null,
    val msg: String? = null,
    val data: T? = null,
)

@Serializable
data class LoginRequest(
    val userName: String,
    val password: String,
    val isParent: Boolean = false,
    val fingerprint: String,
    val redirect: Boolean = false,
    val captchaKey: String? = null,
    val captchaCode: String? = null,
    val userID: Int? = null,
    val qrRequestId: String? = null,
    val otpCode: String? = null,
)

@Serializable
data class LoginPayload(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val require2FA: Boolean? = null,
    val email: String? = null,
)

@Serializable
data class LoginData(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val require2FA: Boolean? = null,
    val email: String? = null,
    val data: LoginPayload? = null,
)

@Serializable
data class UserInfoPayload(
    val user: UserProfile? = null,
)

@Serializable
data class UserProfile(
    val userID: Int? = null,
    val fio: String? = null,
    val fullName: String? = null,
    val group: String? = null,
    @JsonNames("groupID", "groupId", "GroupID")
    val groupID: Int? = null,
    val course: Int? = null,
    val email: String? = null,
)

@Serializable
data class RandomIdentityResponse(
    val randomIdentity: String? = null,
)

@Serializable
data class CurrentUserRasp(
    val id: Int? = null,
    val type: String? = null,
    @JsonNames("groupID", "groupId", "GroupID")
    val groupID: Int? = null,
)

@Serializable
data class ScheduleInfo(
    val curNumNed: Int? = null,
    val curSem: Int? = null,
    val group: ScheduleGroup? = null,
    val typesWeek: List<WeekType>? = null,
)

@Serializable
data class ScheduleGroup(
    val groupID: Int? = null,
    val name: String? = null,
)

@Serializable
data class WeekType(
    val typeWeekID: Int? = null,
    val shortName: String? = null,
    val name: String? = null,
)

@Serializable
data class ScheduleResponse(
    val info: ScheduleInfo? = null,
    val rasp: List<ScheduleLesson> = emptyList(),
)

@Serializable
data class ScheduleLesson(
    @SerialName("Дата") val date: String? = null,
    @SerialName("День_недели") val dayOfWeek: String? = null,
    @SerialName("Дисциплина") val discipline: String? = null,
    @SerialName("Преподаватель") val teacher: String? = null,
    @SerialName("Аудитория") val room: String? = null,
    @SerialName("Начало") val start: String? = null,
    @SerialName("Конец") val end: String? = null,
    @SerialName("НачалоЗанятия") val startDateTime: String? = null,
    @SerialName("ОкончаниеЗанятия") val endDateTime: String? = null,
    @SerialName("НомерЗанятия") val lessonNumber: Int? = null,
    @SerialName("ОтмененоЗанятие") val cancelled: Boolean? = null,
    @SerialName("Цвет") val color: String? = null,
)

@Serializable
data class YearsResponse(
    val years: List<String> = emptyList(),
)

@Serializable
data class GradesResponse(
    val rating: List<SemesterRating> = emptyList(),
    val name: String? = null,
)

@Serializable
data class SemesterRating(
    val sem: Int? = null,
    val year: String? = null,
    val disciplines: List<DisciplineGrade> = emptyList(),
)

@Serializable
data class DisciplineGrade(
    val name: String? = null,
    val teacher: String? = null,
    val controlForm: String? = null,
    val mark: String? = null,
    val avg: String? = null,
    val ratingID: Int? = null,
    val closed: Boolean? = null,
    val type: String? = null,
)

data class GradePoint(
    val title: String,
    val value: String,
)

data class SessionState(
    val token: String?,
    val login: String?,
    val user: UserProfile?,
)

data class UiState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: String? = null,
)
