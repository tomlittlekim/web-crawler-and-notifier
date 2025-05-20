package kr.co.webcrawlerandnotifier.infrastructure.scheduler

import kr.co.webcrawlerandnotifier.application.dto.CrawlingTaskMessage
import kr.co.webcrawlerandnotifier.config.RabbitMQConfig
import kr.co.webcrawlerandnotifier.domain.model.crawler.CrawlerStatus
import kr.co.webcrawlerandnotifier.domain.repository.CrawlerRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock

@Component
class CrawlerScheduler(
    private val crawlerRepository: CrawlerRepository,
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(CrawlerScheduler::class.java)

    // 매 분마다 실행하여 주기가 도래한 크롤러들을 실행 (고정 지연 방식)
    // 실제 운영에서는 분산 환경, DB 부하 등을 고려하여 더 정교한 스케줄링 방식 필요
    @Scheduled(fixedDelayString = "60000") // 1분마다 (60 * 1000 ms)
    @SchedulerLock(name = "scheduledCrawlingTasks", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    fun scheduleCrawlingTasks() {
        logger.info("Running scheduled crawler tasks at ${LocalDateTime.now()}")
        val activeCrawlers = crawlerRepository.findAllByStatus(CrawlerStatus.ACTIVE)

        if (activeCrawlers.isEmpty()) {
            logger.info("No active crawlers to schedule.")
            return
        }

        activeCrawlers.forEach { crawler ->
            // lastCheckedAt이 null인 경우 크롤러의 checkIntervalMs 만큼 과거 시간으로 설정하여 즉시 실행되도록 유도
            // (checkIntervalMs가 Long 타입이므로 toLong()으로 변환)
            val lastChecked = crawler.lastCheckedAt ?: LocalDateTime.now().minusNanos(crawler.checkIntervalMs * 1_000_000L)

            if (lastChecked.plusNanos(crawler.checkIntervalMs * 1_000_000L).isBefore(LocalDateTime.now())) {
                logger.info("Scheduler: Publishing crawling task for crawler id=${crawler.id}, url=${crawler.url}")
                try {
                    val message = CrawlingTaskMessage(crawlerId = crawler.id!!)
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.CRAWLING_EXCHANGE_NAME,
                        RabbitMQConfig.CRAWLING_ROUTING_KEY,
                        message
                    )
                    // 메시지 발행 후 lastCheckedAt을 현재 시간으로 업데이트하여 중복 발행 방지
                    // 실제 크롤링 성공/실패 여부와 관계없이 스케줄러는 발행 책임을 다한 것으로 간주
                    // 실제 크롤링 후 상태 업데이트는 Consumer에서 처리
                    crawler.lastCheckedAt = LocalDateTime.now() // lastCheckedAt 직접 업데이트
                    crawlerRepository.save(crawler)

                } catch (e: Exception) {
                    logger.error("Error publishing crawling task for crawler id=${crawler.id}", e)
                    // 메시지 발행 실패 시 예외 처리 (예: 재시도 로직, 관리자 알림 등)
                }
            }
        }
    }
}