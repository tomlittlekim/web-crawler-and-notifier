package kr.co.webcrawlerandnotifier.domain.statistics

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface StatisticsRepository : JpaRepository<Statistics, Long>, StatisticsRepositoryCustom {

    // 전체 크롤링 시도 횟수 (CRAWL_ATTEMPT 이벤트만) - JPQL로 재정의
    @Query("SELECT COUNT(s) FROM Statistics s WHERE s.eventType = 'CRAWL_ATTEMPT'")
    fun countCrawlAttempts(): Long

    // 전체 크롤링 성공 횟수 - JPQL로 재정의
    @Query("SELECT COUNT(s) FROM Statistics s WHERE s.eventType = 'CRAWL_ATTEMPT' AND s.isSuccess = true")
    fun countSuccessfulCrawlAttempts(): Long
}

// JPQL 프로젝션용 데이터 클래스들은 StatisticsRepositoryImpl에서 QueryDSL 결과 매핑에 사용되므로 유지합니다.
data class EventTypeProjection(
    val eventType: String,
    val totalAttempts: Long,
    val successCount: Long
)

data class UrlCrawlStatProjection(
    val targetUrl: String,
    val totalAttempts: Long,
    val successCount: Long,
    val failureCount: Long,
    val averageDurationMsSuccess: Double?,
    val lastFailureTimestamp: LocalDateTime?,
    val lastCheckedAt: LocalDateTime?
) 