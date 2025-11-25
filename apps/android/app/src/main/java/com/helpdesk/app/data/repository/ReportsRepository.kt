package com.helpdesk.app.data.repository

import com.helpdesk.app.data.api.ReportsApi
import com.helpdesk.app.domain.model.*
import com.helpdesk.app.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportsRepository @Inject constructor(
    private val reportsApi: ReportsApi
) {
    suspend fun getStatusSummary(): Result<TicketSummaryReport> {
        return try {
            val response = reportsApi.getStatusSummary()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.summary.toDomain())
            } else {
                Result.Error(Exception("Failed to fetch status summary: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getRecentActivity(limit: Int = 25): Result<List<TicketActivityEntry>> {
        return try {
            val response = reportsApi.getRecentActivity(limit)
            if (response.isSuccessful && response.body() != null) {
                val activities = response.body()!!.activities.map { it.toDomain() }
                Result.Success(activities)
            } else {
                Result.Error(Exception("Failed to fetch activity: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUserTicketReport(): Result<UserTicketReport> {
        return try {
            val response = reportsApi.getUserTicketReport()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.report.toDomain())
            } else {
                Result.Error(Exception("Failed to fetch user report: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAgentWorkloadReport(): Result<AgentWorkloadReport> {
        return try {
            val response = reportsApi.getAgentWorkloadReport()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.report.toDomain())
            } else {
                Result.Error(Exception("Failed to fetch agent workload: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAdminOverviewReport(): Result<AdminOverviewReport> {
        return try {
            val response = reportsApi.getAdminOverviewReport()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.report.toDomain())
            } else {
                Result.Error(Exception("Failed to fetch admin overview: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getAdminEscalationReport(): Result<AdminEscalationReport> {
        return try {
            val response = reportsApi.getAdminEscalationReport()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.report.toDomain())
            } else {
                Result.Error(Exception("Failed to fetch escalation report: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
