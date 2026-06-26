package ru.kgeu.lk.data.api

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ru.kgeu.lk.data.model.ApiEnvelope
import ru.kgeu.lk.data.model.CurrentUserRasp
import ru.kgeu.lk.data.model.LoginRequest
import ru.kgeu.lk.data.model.RandomIdentityResponse
import ru.kgeu.lk.data.model.UserInfoPayload
import ru.kgeu.lk.data.model.YearsResponse

interface KgeuApi {
    @POST("/api/tokenauth")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<LoginResponseData>

    @GET("/api/tokenauth")
    suspend fun getUserInfo(): ApiEnvelope<UserInfoPayload>

    @GET("/api/UserInfo/Devices/RandomIdentity")
    suspend fun randomIdentity(): RandomIdentityResponse

    @GET("/api/CurrentUserRasp")
    suspend fun currentUserRasp(): ApiEnvelope<CurrentUserRasp>

    @GET("/api/Rasp/ListYears")
    suspend fun listYears(): ApiEnvelope<YearsResponse>

    @GET("/api/Rasp")
    suspend fun schedule(
        @Query("idGroup") groupId: Int,
        @Query("sdate") date: String,
        @Query("year") year: String,
    ): ApiEnvelope<JsonObject>

    @GET("/api/SchoolX/Feedback/Stat/IntermediatePerfomance")
    suspend fun grades(
        @Query("onlyDebts") onlyDebts: Boolean = false,
    ): ApiEnvelope<JsonObject>
}

@kotlinx.serialization.Serializable
data class LoginResponseData(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val require2FA: Boolean? = null,
    val email: String? = null,
    val data: JsonElement? = null,
)
