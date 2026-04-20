package com.example.clientforwebstorage.network
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CreateFolderRequest
import com.example.clientforwebstorage.network.models.LoginRequest
import com.example.clientforwebstorage.network.models.PurgeRecycleRequest
import com.example.clientforwebstorage.network.models.RenameRequest
import com.example.clientforwebstorage.network.models.RegisterRequest
import com.example.clientforwebstorage.network.models.UploadCompleteRequest
import com.example.clientforwebstorage.network.models.UploadInitRequest
import com.example.clientforwebstorage.network.models.VerificationRequest
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    @GET("api/v1/resources/recycle")
    fun getRecycleResources(
        @Query("keyword") keyword: String?,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @POST("api/v1/resources/folders")
    fun createFolder(@Body request: CreateFolderRequest): Call<ApiResponse>

    @PATCH("api/v1/resources/{resourceId}")
    fun renameResource(
        @Path("resourceId") resourceId: String,
        @Body request: RenameRequest
    ): Call<ApiResponse>

    @DELETE("api/v1/resources/{resourceId}")
    fun deleteResource(
        @Path("resourceId") resourceId: String
    ): Call<ApiResponse>

    @POST("api/v1/resources/{resourceId}/restore")
    fun restoreResource(
        @Path("resourceId") resourceId: String
    ): Call<ApiResponse>

    @POST("api/v1/resources/recycle/purge")
    fun purgeRecycle(
        @Body request: PurgeRecycleRequest
    ): Call<ApiResponse>

    @GET("api/v1/me/activities")
    fun getUserActivities(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @POST("api/v1/uploads/init")
    fun initUpload(@Body request: UploadInitRequest): Call<ApiResponse>

    @PUT("api/v1/uploads/{uploadId}/parts/{partNumber}")
    fun uploadPart(
        @Path("uploadId") uploadId: String,
        @Path("partNumber") partNumber: Int,
        @Body body: RequestBody
    ): Call<ApiResponse>

    @POST("api/v1/uploads/{uploadId}/complete")
    fun completeUpload(
        @Path("uploadId") uploadId: String,
        @Body request: UploadCompleteRequest
    ): Call<ApiResponse>

    @GET("api/v1/uploads/{uploadId}")
    fun getUploadStatus(@Path("uploadId") uploadId: String): Call<ApiResponse>

    @DELETE("api/v1/uploads/{uploadId}")
    fun cancelUpload(@Path("uploadId") uploadId: String): Call<ApiResponse>
}