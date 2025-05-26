package kr.co.webcrawlerandnotifier.domain.service

import kr.co.webcrawlerandnotifier.domain.model.crawler.Crawler
import kr.co.webcrawlerandnotifier.domain.model.log.CrawlLog
import kr.co.webcrawlerandnotifier.domain.repository.CrawlLogRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 크롤링 관련 도메인 서비스
 * 
 * 크롤링과 관련된 복잡한 비즈니스 로직을 담당합니다.
 * 애플리케이션 서비스에서 이 도메인 서비스를 사용하여 
 * 도메인 로직과 애플리케이션 로직을 분리합니다.
 * 
 * 주요 책임:
 * - 크롤링 로그 생성 및 관리
 * - 알림 발송 조건 판단
 * - 크롤링 결과 분석
 */
@Service
class CrawlingService(
    private val crawlLogRepository: CrawlLogRepository
) {
    
    /**
     * 크롤링 로그를 생성하고 저장합니다.
     * 
     * @param crawler 크롤러 엔티티
     * @param crawledValue 크롤링된 값
     * @param success 성공 여부
     * @param errorMessage 에러 메시지 (실패 시)
     * @param notificationSent 알림 발송 여부
     * @return 저장된 크롤링 로그
     */
    fun createCrawlLog(
        crawler: Crawler,
        crawledValue: String?,
        success: Boolean,
        errorMessage: String? = null,
        notificationSent: Boolean = false
    ): CrawlLog {
        val crawlLog = CrawlLog(
            crawler = crawler,
            crawledAt = LocalDateTime.now(),
            crawledValue = crawledValue,
            success = success,
            errorMessage = errorMessage,
            notificationSent = notificationSent
        )
        return crawlLogRepository.save(crawlLog)
    }

    /**
     * 주어진 크롤링 결과에 대해 알림을 발송해야 하는지 판단합니다.
     * 
     * 알림 발송 조건:
     * 1. 키워드가 설정되어 있고, 크롤링된 값에 해당 키워드가 포함된 경우
     * 2. 변경 감지가 활성화되어 있고, 이전 값과 다른 경우
     * 
     * @param crawler 크롤러 엔티티
     * @param newValue 새로 크롤링된 값
     * @return 알림 발송 여부
     */
    fun shouldTriggerNotification(crawler: Crawler, newValue: String?): Boolean {
        // 리팩토링된 Crawler의 메서드 사용
        return crawler.shouldNotify(newValue)
    }

    /**
     * 알림을 발송하는 이유를 문자열 리스트로 반환합니다.
     * 
     * @param crawler 크롤러 엔티티
     * @param newValue 새로 크롤링된 값
     * @return 알림 이유 목록
     */
    fun getNotificationReasons(crawler: Crawler, newValue: String?): List<String> {
        // 리팩토링된 Crawler의 메서드 사용
        return crawler.getNotificationReasons(newValue)
    }
    
    /**
     * 크롤링 결과를 분석하여 요약 정보를 반환합니다.
     * 
     * @param crawler 크롤러 엔티티
     * @param newValue 새로 크롤링된 값
     * @return 크롤링 결과 요약
     */
    fun analyzeCrawlingResult(crawler: Crawler, newValue: String?): CrawlingResultSummary {
        val hasChanged = crawler.state.lastCrawledValue != newValue
        val shouldNotify = shouldTriggerNotification(crawler, newValue)
        val reasons = if (shouldNotify) getNotificationReasons(crawler, newValue) else emptyList()
        
        return CrawlingResultSummary(
            hasChanged = hasChanged,
            shouldNotify = shouldNotify,
            notificationReasons = reasons,
            previousValue = crawler.state.lastCrawledValue,
            newValue = newValue
        )
    }
    
    /**
     * 크롤러의 상태를 업데이트합니다.
     * 
     * @param crawler 크롤러 엔티티
     * @param newValue 새로 크롤링된 값
     * @param success 크롤링 성공 여부
     * @param errorMessage 에러 메시지 (실패 시)
     */
    fun updateCrawlerState(
        crawler: Crawler, 
        newValue: String?, 
        success: Boolean, 
        errorMessage: String? = null
    ) {
        if (success) {
            crawler.updateCrawledData(newValue)
        } else {
            crawler.markAsError(errorMessage ?: "알 수 없는 오류")
        }
    }
}

/**
 * 크롤링 결과 요약 정보를 담는 데이터 클래스
 * 
 * @property hasChanged 내용이 변경되었는지 여부
 * @property shouldNotify 알림을 발송해야 하는지 여부
 * @property notificationReasons 알림 발송 이유 목록
 * @property previousValue 이전 값
 * @property newValue 새로운 값
 */
data class CrawlingResultSummary(
    val hasChanged: Boolean,
    val shouldNotify: Boolean,
    val notificationReasons: List<String>,
    val previousValue: String?,
    val newValue: String?
) 