package kr.co.webcrawlerandnotifier.application.statistics

import kr.co.webcrawlerandnotifier.application.dto.statistics.* // DTO 임포트
import kr.co.webcrawlerandnotifier.domain.statistics.Statistics
import kr.co.webcrawlerandnotifier.domain.statistics.StatisticsRepository
import org.springframework.data.domain.PageRequest // PageRequest 임포트
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class StatisticsService(
    private val statisticsRepository: StatisticsRepository
) {

    @Transactional
    fun recordEvent(
        eventType: String,
        targetUrl: String? = null,
        durationMs: Long? = null,
        isSuccess: Boolean,
        details: String? = null
    ) {
        val statistics = Statistics(
            eventTimestamp = LocalDateTime.now(),
            eventType = eventType,
            targetUrl = targetUrl,
            durationMs = durationMs,
            isSuccess = isSuccess,
            details = details
        )
        statisticsRepository.save(statistics)
    }

    @Transactional(readOnly = true)
    fun getOverallCrawlStats(): OverallStatsDto {
        val totalAttempts = statisticsRepository.countCrawlAttempts()
        val successCount = statisticsRepository.countSuccessfulCrawlAttempts()
        val failureCount = totalAttempts - successCount
        val successRate = if (totalAttempts > 0) (successCount.toDouble() / totalAttempts.toDouble()) * 100 else 0.0
        return OverallStatsDto(totalAttempts, successCount, failureCount, successRate)
    }

    @Transactional(readOnly = true)
    fun getEventTypeSummaries(): List<EventTypeSummaryDto> {
        return statisticsRepository.getEventTypeSummariesWithQueryDsl().map { projection ->
            val failureCount = projection.totalAttempts - projection.successCount
            val successRate = if (projection.totalAttempts > 0) (projection.successCount.toDouble() / projection.totalAttempts.toDouble()) * 100 else 0.0
            EventTypeSummaryDto(
                eventType = projection.eventType,
                totalAttempts = projection.totalAttempts,
                successCount = projection.successCount,
                failureCount = failureCount,
                successRate = successRate
            )
        }
    }

    @Transactional(readOnly = true)
    fun getRecentErrors(limit: Int = 10): List<RecentErrorDto> {
        val pageable = PageRequest.of(0, limit)
        return statisticsRepository.findRecentErrorsWithQueryDsl(pageable).map { stat ->
            RecentErrorDto(
                eventTimestamp = stat.eventTimestamp,
                eventType = stat.eventType,
                targetUrl = stat.targetUrl,
                details = stat.details
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUrlCrawlStats(): List<UrlCrawlStatsDto> {
        return statisticsRepository.getUrlCrawlStatsWithQueryDsl().map { projection ->
            val successRate = if (projection.totalAttempts > 0) (projection.successCount.toDouble() / projection.totalAttempts.toDouble()) * 100 else 0.0
            UrlCrawlStatsDto(
                targetUrl = projection.targetUrl,
                totalAttempts = projection.totalAttempts,
                successCount = projection.successCount,
                failureCount = projection.failureCount,
                successRate = successRate,
                averageDurationMsSuccess = projection.averageDurationMsSuccess,
                lastFailureTimestamp = projection.lastFailureTimestamp,
                lastCheckedAt = projection.lastCheckedAt
            )
        }
    }

    // 향후 대시보드용 조회 메서드 추가 예정
    // 예: fun getCrawlingSuccessRate(): Double
    // 예: fun getEventsByType(eventType: String): List<Statistics>
} 