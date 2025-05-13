package kr.co.webcrawlerandnotifier.infrastructure.scheduler

import kr.co.webcrawlerandnotifier.application.service.CrawlerAppService
import kr.co.webcrawlerandnotifier.domain.model.crawler.CrawlerStatus
import kr.co.webcrawlerandnotifier.domain.repository.CrawlerRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CrawlerScheduler(
    private val crawlerRepository: CrawlerRepository,
    private val crawlerAppService: CrawlerAppService // 실제 크롤링 로직은 AppService에 위임
) {
    private val logger = LoggerFactory.getLogger(CrawlerScheduler::class.java)

    // 매 분마다 실행하여 주기가 도래한 크롤러들을 실행 (고정 지연 방식)
    // 실제 운영에서는 분산 환경, DB 부하 등을 고려하여 더 정교한 스케줄링 방식 필요
    @Scheduled(fixedDelayString = "60000") // 1분마다 (60 * 1000 ms)
    fun scheduleCrawlingTasks() {
        logger.info("Running scheduled crawler tasks at ${LocalDateTime.now()}")
        val activeCrawlers = crawlerRepository.findAllByStatus(CrawlerStatus.ACTIVE)

        if (activeCrawlers.isEmpty()) {
            logger.info("No active crawlers to schedule.")
            return
        }

        activeCrawlers.forEach { crawler ->
            val now = LocalDateTime.now()
            val lastChecked = crawler.lastCheckedAt ?: now.minusSeconds(crawler.checkIntervalMs / 1000) // 최초 실행 또는 오래된 경우

            if (lastChecked.plusNanos(crawler.checkIntervalMs * 1_000_000).isBefore(now)) {
                logger.info("Scheduler: Triggering crawler id=${crawler.id}, url=${crawler.url}")
                try {
                    // 비동기 실행 고려 (예: @Async 또는 별도 스레드 풀)
                    // 여기서는 간단히 동기 호출
                    crawlerAppService.executeCrawlingAndNotification(crawler)
                } catch (e: Exception) {
                    logger.error("Error during scheduled execution of crawler id=${crawler.id}", e)
                    // 여기서 crawler 상태를 ERROR로 변경할 수도 있으나, executeCrawlingAndNotification 내부에서 처리
                }
            }
        }
    }
}