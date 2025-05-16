package kr.co.webcrawlerandnotifier.application.dto.statistics

import java.time.LocalDateTime

data class OverallStatsDto(
    val totalAttempts: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Double
)

data class EventTypeSummaryDto(
    val eventType: String,
    val totalAttempts: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Double
)

data class RecentErrorDto(
    val eventTimestamp: LocalDateTime,
    val eventType: String,
    val targetUrl: String?,
    val details: String?
)

data class UrlCrawlStatsDto(
    val targetUrl: String,
    val totalAttempts: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Double,
    val averageDurationMsSuccess: Double?, // 성공한 경우에만 평균 시간
    val lastFailureTimestamp: LocalDateTime?, // lastErrorMessage에서 변경
    val lastCheckedAt: LocalDateTime?
) 