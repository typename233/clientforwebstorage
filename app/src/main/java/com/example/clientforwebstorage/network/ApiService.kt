package com.example.clientforwebstorage.network
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CreateFolderRequest
import com.example.clientforwebstorage.network.models.CreateGroupInviteRequest
import com.example.clientforwebstorage.network.models.CreateGroupRequest
import com.example.clientforwebstorage.network.models.CreatePreviewJobRequest
import com.example.clientforwebstorage.network.models.CreateShareRequest
import com.example.clientforwebstorage.network.models.CreateTagRequest
import com.example.clientforwebstorage.network.models.CompleteVersionUploadRequest
import com.example.clientforwebstorage.network.models.InitVersionUploadRequest
import com.example.clientforwebstorage.network.models.LoginRequest
import com.example.clientforwebstorage.network.models.PurgeRecycleRequest
import com.example.clientforwebstorage.network.models.RefreshTokenRequest
import com.example.clientforwebstorage.network.models.RenameRequest
import com.example.clientforwebstorage.network.models.RegisterRequest
import com.example.clientforwebstorage.network.models.SubmitJoinRequest
import com.example.clientforwebstorage.network.models.UpdateGroupRequest
import com.example.clientforwebstorage.network.models.UpdateMemberRoleRequest
import com.example.clientforwebstorage.network.models.UpdateShareRequest
import com.example.clientforwebstorage.network.models.UpdateTagRequest
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

    @POST("api/v1/auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<ApiResponse>

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

    @GET("api/v1/resources/{resourceId}/preview-url")
    fun getPreviewUrl(
        @Path("resourceId") resourceId: String
    ): Call<ApiResponse>

    @GET("api/v1/resources/{resourceId}/download-url")
    fun getDownloadUrl(
        @Path("resourceId") resourceId: String
    ): Call<ApiResponse>

    @POST("api/v1/shares")
    fun createShare(
        @Body request: CreateShareRequest
    ): Call<ApiResponse>

    @GET("api/v1/shares")
    fun getShareList(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @POST("api/v1/shares/{shareId}/revoke")
    fun revokeShare(
        @Path("shareId") shareId: String
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

    @POST("api/v2/groups")
    fun createGroup(@Body request: CreateGroupRequest): Call<ApiResponse>

    @GET("api/v2/groups")
    fun getGroups(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("keyword") keyword: String?
    ): Call<ApiResponse>

    @GET("api/v2/groups/{groupId}")
    fun getGroupDetail(@Path("groupId") groupId: String): Call<ApiResponse>

    @PATCH("api/v2/groups/{groupId}")
    fun updateGroup(
        @Path("groupId") groupId: String,
        @Body request: UpdateGroupRequest
    ): Call<ApiResponse>

    @DELETE("api/v2/groups/{groupId}")
    fun deleteGroup(@Path("groupId") groupId: String): Call<ApiResponse>

    @POST("api/v2/groups/{groupId}/archive")
    fun archiveGroup(@Path("groupId") groupId: String): Call<ApiResponse>

    @GET("api/v2/groups/{groupId}/members")
    fun getGroupMembers(
        @Path("groupId") groupId: String,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("keyword") keyword: String?
    ): Call<ApiResponse>

    @PATCH("api/v2/groups/{groupId}/members/{userId}")
    fun updateMemberRole(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String,
        @Body request: UpdateMemberRoleRequest
    ): Call<ApiResponse>

    @DELETE("api/v2/groups/{groupId}/members/{userId}")
    fun removeMember(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String
    ): Call<ApiResponse>

    @POST("api/v2/groups/{groupId}/invites")
    fun inviteMember(
        @Path("groupId") groupId: String,
        @Body request: CreateGroupInviteRequest
    ): Call<ApiResponse>

    @GET("api/v2/groups/{groupId}/invites")
    fun getInvites(
        @Path("groupId") groupId: String,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("status") status: String?
    ): Call<ApiResponse>

    @POST("api/v2/groups/{groupId}/invites/{inviteId}/cancel")
    fun cancelInvite(
        @Path("groupId") groupId: String,
        @Path("inviteId") inviteId: String
    ): Call<ApiResponse>

    @POST("api/v2/group-invites/{inviteToken}/accept")
    fun acceptInvite(@Path("inviteToken") inviteToken: String): Call<ApiResponse>

    @POST("api/v2/group-invites/{inviteToken}/reject")
    fun rejectInvite(@Path("inviteToken") inviteToken: String): Call<ApiResponse>

    @POST("api/v2/groups/{groupId}/join-requests")
    fun submitJoinRequest(
        @Path("groupId") groupId: String,
        @Body request: SubmitJoinRequest? = null
    ): Call<ApiResponse>

    @GET("api/v2/groups/{groupId}/join-requests")
    fun getGroupJoinRequests(
        @Path("groupId") groupId: String,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("status") status: String?
    ): Call<ApiResponse>

    @GET("api/v2/me/group-join-requests")
    fun getMyJoinRequests(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @POST("api/v2/groups/{groupId}/join-requests/{requestId}/approve")
    fun approveJoinRequest(
        @Path("groupId") groupId: String,
        @Path("requestId") requestId: String
    ): Call<ApiResponse>

    @POST("api/v2/groups/{groupId}/join-requests/{requestId}/reject")
    fun rejectJoinRequest(
        @Path("groupId") groupId: String,
        @Path("requestId") requestId: String
    ): Call<ApiResponse>

    @POST("api/v2/resources/{resourceId}/favorite")
    fun favoriteResource(@Path("resourceId") resourceId: String): Call<ApiResponse>

    @DELETE("api/v2/resources/{resourceId}/favorite")
    fun unfavoriteResource(@Path("resourceId") resourceId: String): Call<ApiResponse>

    @GET("api/v2/me/favorites")
    fun getFavorites(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @POST("api/v2/tags")
    fun createTag(@Body request: CreateTagRequest): Call<ApiResponse>

    @GET("api/v2/tags")
    fun getTags(): Call<ApiResponse>

    @PATCH("api/v2/tags/{tagId}")
    fun updateTag(
        @Path("tagId") tagId: String,
        @Body request: UpdateTagRequest
    ): Call<ApiResponse>

    @DELETE("api/v2/tags/{tagId}")
    fun deleteTag(@Path("tagId") tagId: String): Call<ApiResponse>

    @GET("api/v2/resources/{resourceId}/tags")
    fun getResourceTags(@Path("resourceId") resourceId: String): Call<ApiResponse>

    @POST("api/v2/resources/{resourceId}/tags/{tagId}")
    fun addResourceTag(
        @Path("resourceId") resourceId: String,
        @Path("tagId") tagId: String
    ): Call<ApiResponse>

    @DELETE("api/v2/resources/{resourceId}/tags/{tagId}")
    fun removeResourceTag(
        @Path("resourceId") resourceId: String,
        @Path("tagId") tagId: String
    ): Call<ApiResponse>

    @GET("api/v2/resources/{resourceId}/versions")
    fun getResourceVersions(
        @Path("resourceId") resourceId: String,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @POST("api/v2/resources/{resourceId}/versions/init")
    fun initVersionUpload(
        @Path("resourceId") resourceId: String,
        @Body request: InitVersionUploadRequest
    ): Call<ApiResponse>

    @PUT("api/v2/resources/{resourceId}/versions/{uploadId}/parts/{partNumber}")
    fun uploadVersionPart(
        @Path("resourceId") resourceId: String,
        @Path("uploadId") uploadId: String,
        @Path("partNumber") partNumber: Int,
        @Body body: RequestBody
    ): Call<ApiResponse>

    @POST("api/v2/resources/{resourceId}/versions/{uploadId}/complete")
    fun completeVersionUpload(
        @Path("resourceId") resourceId: String,
        @Path("uploadId") uploadId: String,
        @Body request: CompleteVersionUploadRequest? = null
    ): Call<ApiResponse>

    @POST("api/v2/resources/{resourceId}/versions/{versionNo}/restore")
    fun restoreVersion(
        @Path("resourceId") resourceId: String,
        @Path("versionNo") versionNo: Int
    ): Call<ApiResponse>

    @GET("api/v2/resources/{resourceId}/versions/{versionNo}/download-url")
    fun getVersionDownloadUrl(
        @Path("resourceId") resourceId: String,
        @Path("versionNo") versionNo: Int
    ): Call<ApiResponse>

    @POST("api/v2/resources/{resourceId}/preview-jobs")
    fun createPreviewJob(
        @Path("resourceId") resourceId: String,
        @Body request: CreatePreviewJobRequest
    ): Call<ApiResponse>

    @GET("api/v2/preview-jobs/{jobId}")
    fun getPreviewJob(@Path("jobId") jobId: String): Call<ApiResponse>

    @GET("api/v2/preview-jobs")
    fun getPreviewJobs(
        @Query("status") status: String?,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @PATCH("api/v2/shares/{shareId}")
    fun updateShare(
        @Path("shareId") shareId: String,
        @Body request: UpdateShareRequest
    ): Call<ApiResponse>

    @GET("api/v2/shares/{shareId}/access-logs")
    fun getShareAccessLogs(
        @Path("shareId") shareId: String,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @GET("api/v2/shares/{shareId}/stats")
    fun getShareStats(@Path("shareId") shareId: String): Call<ApiResponse>

    @GET("api/v2/notifications")
    fun getNotifications(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("unreadOnly") unreadOnly: Boolean? = false
    ): Call<ApiResponse>

    @POST("api/v2/notifications/{notificationId}/read")
    fun markNotificationRead(@Path("notificationId") notificationId: String): Call<ApiResponse>

    @POST("api/v2/notifications/read-all")
    fun markAllNotificationsRead(): Call<ApiResponse>

    @DELETE("api/v2/notifications/{notificationId}")
    fun deleteNotification(@Path("notificationId") notificationId: String): Call<ApiResponse>

    @GET("api/v2/public/groups/{groupId}")
    fun getPublicGroupInfo(@Path("groupId") groupId: String): Call<ApiResponse>

    @GET("api/v2/public/groups/{groupId}/resources")
    fun getPublicGroupResources(
        @Path("groupId") groupId: String,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @GET("api/v2/groups/{groupId}/messages")
    fun getGroupMessages(
        @Path("groupId") groupId: String,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): Call<ApiResponse>

    @POST("api/v2/groups/{groupId}/messages")
    fun sendGroupMessage(
        @Path("groupId") groupId: String,
        @Body request: com.example.clientforwebstorage.network.models.SendMessageRequest
    ): Call<ApiResponse>
}