package kr.co.webcrawlerandnotifier.infrastructure.messaging

import kr.co.webcrawlerandnotifier.application.dto.CrawlingTaskMessage
import kr.co.webcrawlerandnotifier.application.service.CrawlerAppService
import kr.co.webcrawlerandnotifier.config.RabbitMQConfig
import kr.co.webcrawlerandnotifier.domain.repository.CrawlerRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CrawlingMessageListener(
    private val crawlerAppService: CrawlerAppService,
    private val crawlerRepository: CrawlerRepository // Crawler 엔티티를 직접 조회하기 위해 추가
) {
    private val logger = LoggerFactory.getLogger(CrawlingMessageListener::class.java)

    @Transactional // 크롤링 및 알림, 로그 저장, 상태 업데이트를 하나의 트랜잭션으로 처리
    @RabbitListener(queues = [RabbitMQConfig.CRAWLING_QUEUE_NAME])
    fun handleCrawlingTask(message: CrawlingTaskMessage) {
        logger.info("Received crawling task for crawler id=${message.crawlerId}")
        try {
            val crawler = crawlerRepository.findById(message.crawlerId)
                .orElseThrow { RuntimeException("Crawler not found with id: ${message.crawlerId}") }

            // 스케줄러에서 이미 lastCheckedAt을 업데이트 했으므로, 여기서는 실제 크롤링/알림 로직만 수행
            // 또는, 스케줄러에서 lastCheckedAt 업데이트 로직을 제거하고 여기서 처리하는 것도 고려 가능
            // 현재는 스케줄러가 발행 책임을 지고, 컨슈머가 실행 책임을 지는 구조
            crawlerAppService.executeCrawlingAndNotification(crawler)
            logger.info("Successfully processed crawling task for crawler id=${message.crawlerId}")
        } catch (e: Exception) {
            // 메시지 처리 실패 시 로깅. RabbitMQ의 기본 재시도 메커니즘 또는 Dead Letter Queue(DLQ) 설정에 따라 처리됨.
            // 여기서는 간단히 에러 로깅만 수행.
            // 필요시 특정 예외에 따라 수동으로 메시지를 acknowledge 하거나 reject 할 수 있음.
            logger.error("Error processing crawling task for crawler id=${message.crawlerId}", e)
            // 여기서 해당 crawler의 상태를 ERROR로 변경하거나, 실패 로그를 기록하는 등의 추가 작업도 가능
            // 예를 들어, crawlerAppService.markCrawlerAsError(message.crawlerId, e.message) 와 같은 메소드 호출
        }
    }
} 