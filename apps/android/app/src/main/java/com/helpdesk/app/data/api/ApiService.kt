package com.helpdesk.app.data.api

import com.helpdesk.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<AuthResponse>
}

interface TicketsApi {
    @GET("api/tickets")
    suspend fun getTickets(
        @Query("status") status: String? = null,
        @Query("issueType") issueType: String? = null,
        @Query("assignedToMe") assignedToMe: Boolean? = null,
        @Query("limit") limit: Int? = null
    ): Response<TicketListResponse>

    @GET("api/tickets/{ticketId}")
    suspend fun getTicket(@Path("ticketId") ticketId: String): Response<TicketResponse>

    @POST("api/tickets")
    suspend fun createTicket(@Body request: CreateTicketRequest): Response<TicketResponse>

    @PATCH("api/tickets/{ticketId}")
    suspend fun updateTicket(
        @Path("ticketId") ticketId: String,
        @Body request: UpdateTicketRequest
    ): Response<TicketResponse>

    @POST("api/tickets/{ticketId}/assign")
    suspend fun assignTicket(
        @Path("ticketId") ticketId: String,
        @Body request: AssignTicketRequest
    ): Response<TicketResponse>

    @POST("api/tickets/{ticketId}/resolve")
    suspend fun resolveTicket(@Path("ticketId") ticketId: String): Response<TicketResponse>

    @POST("api/tickets/{ticketId}/request-assignment")
    suspend fun requestAssignment(@Path("ticketId") ticketId: String): Response<TicketResponse>

    @POST("api/tickets/{ticketId}/assignment-request/decline")
    suspend fun declineAssignmentRequest(@Path("ticketId") ticketId: String): Response<TicketResponse>

    @GET("api/tickets/{ticketId}/activity")
    suspend fun getTicketActivity(
        @Path("ticketId") ticketId: String,
        @Query("limit") limit: Int? = null
    ): Response<TicketActivityListResponse>
}

interface ReportsApi {
    @GET("api/reports/tickets/status-summary")
    suspend fun getStatusSummary(): Response<StatusSummaryResponse>

    @GET("api/reports/tickets/activity")
    suspend fun getRecentActivity(
        @Query("limit") limit: Int? = null
    ): Response<TicketActivityListResponse>

    @GET("api/reports/users/me/tickets")
    suspend fun getUserTicketReport(): Response<UserTicketReportResponse>

    @GET("api/reports/agents/me/workload")
    suspend fun getAgentWorkloadReport(): Response<AgentWorkloadReportResponse>

    @GET("api/reports/admin/overview")
    suspend fun getAdminOverviewReport(): Response<AdminOverviewReportResponse>

    @GET("api/reports/admin/escalations")
    suspend fun getAdminEscalationReport(): Response<AdminEscalationReportResponse>
}

interface UsersApi {
    @GET("api/users")
    suspend fun getUsers(@Query("role") role: String? = null): Response<UserListResponse>

    @POST("api/users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<UserResponse>

    @PATCH("api/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>

    @DELETE("api/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<UserResponse>
}
