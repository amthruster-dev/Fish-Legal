package com.fishlegal.app.network

import com.fishlegal.app.data.RegulationRule
import com.fishlegal.app.data.RegulationZone
import com.fishlegal.app.data.Species
import retrofit2.Response
import retrofit2.http.*

/**
 * Backend endpoints for auto-sync: pulling updated species/model/regulations
 * data, and pushing queued catch logs, whenever the device is online.
 */
interface ApiService {

    @GET("v1/species")
    suspend fun getSpeciesUpdates(@Query("since") since: Long): Response<List<Species>>

    @GET("v1/regulations/zones")
    suspend fun getZoneUpdates(@Query("since") since: Long): Response<List<RegulationZone>>

    @GET("v1/regulations/rules")
    suspend fun getRuleUpdates(@Query("since") since: Long): Response<List<RegulationRule>>

    @GET("v1/model/latest")
    suspend fun getLatestModelInfo(): Response<ModelInfo>

    @Streaming
    @GET
    suspend fun downloadModelFile(@Url url: String): Response<okhttp3.ResponseBody>

    @Multipart
    @POST("v1/catches")
    suspend fun uploadCatch(
        @Part photo: okhttp3.MultipartBody.Part,
        @Part("speciesId") speciesId: okhttp3.RequestBody,
        @Part("lat") lat: okhttp3.RequestBody,
        @Part("lon") lon: okhttp3.RequestBody,
        @Part("timestamp") timestamp: okhttp3.RequestBody
    ): Response<Unit>
}

data class ModelInfo(
    val version: Int,
    val modelUrl: String,
    val labelsUrl: String
)

object ApiClient {
    private const val BASE_URL = "https://api.fishlegal.app/" // placeholder backend

    val service: ApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
