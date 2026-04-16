package com.example.clientforwebstorage.network
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.LoginRequest
import com.example.clientforwebstorage.network.models.RegisterRequest
import com.example.clientforwebstorage.network.models.VerificationRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/v1/auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse>

    @POST("api/v1/auth/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse>

    @POST("api/v1/auth/verification/send")
    fun sendVerificationCode(
        @Body request: VerificationRequest
    ): Call<ApiResponse>

    @GET("api/v1/resources")
    fun getResources(
        @Query("parentId") parentId: String?,
        @Query("keyword") keyword: String?,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>
}