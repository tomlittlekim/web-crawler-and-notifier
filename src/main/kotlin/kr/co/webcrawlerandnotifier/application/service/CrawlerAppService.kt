package kr.co.webcrawlerandnotifier.application.service

import kr.co.webcrawlerandnotifier.application.dto.*
import kr.co.webcrawlerandnotifier.domain.exception.CrawlerNotFoundException
import kr.co.webcrawlerandnotifier.domain.model.crawler.Crawler
import kr.co.webcrawlerandnotifier.domain.model.crawler.CrawlerStatus
import kr.co.webcrawlerandnotifier.domain.model.log.CrawlLog
import kr.co.webcrawlerandnotifier.domain.repository.CrawlLogRepository
import kr.co.webcrawlerandnotifier.domain.repository.CrawlerRepository
import kr.co.webcrawlerandnotifier.infrastructure.crawling.WebCrawlerService // 인터페이스 대신 구현체 직접 사용 (간단하게)
import kr.co.webcrawlerandnotifier.infrastructure.notification.NotificationService // 인터페이스 대신 구현체 직접 사용 (간단하게)
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class CrawlerAppService(
    private val crawlerRepository: CrawlerRepository,
    private val crawlLogRepository: CrawlLogRepository,
    private val webCrawlerService: WebCrawlerService, // 실제 크롤링 서비스
    private val notificationService: NotificationService // 알림 서비스
) {
    private val logger = LoggerFactory.getLogger(CrawlerAppService::class.java)

    @Transactional
    fun createCrawler(request: CreateCrawlerRequest): CrawlerResponse {
        val crawler = Crawler(
            url = request.url,
            selector = request.selector,
            checkIntervalMs = request.checkInterval,
            alertKeyword = request.alertKeyword,
            alertOnChange = request.alertOnChange,
            email = request.email,
            status = CrawlerStatus.ACTIVE // 생성 시 기본 활성 상태
        )
        val savedCrawler = crawlerRepository.save(crawler)
        logger.info("Crawler created: id=${savedCrawler.id}, url=${savedCrawler.url}")
        return CrawlerResponse.fromEntity(savedCrawler)
    }

    @Transactional(readOnly = true)
    fun getAllCrawlers(): List<CrawlerResponse> {
        return crawlerRepository.findAll().map { CrawlerResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getCrawlerById(id: UUID): CrawlerResponse {
        val crawler = crawlerRepository.findById(id)
            .orElseThrow { CrawlerNotFoundException(id) }
        return CrawlerResponse.fromEntity(crawler)
    }

    @Transactional
    fun updateCrawler(id: UUID, request: UpdateCrawlerRequest): CrawlerResponse {
        val crawler = crawlerRepository.findById(id)
            .orElseThrow { CrawlerNotFoundException(id) }

        crawler.updateDetails(
            url = request.url,
            selector = request.selector,
            checkIntervalMs = request.checkInterval,
            alertKeyword = request.alertKeyword,
            alertOnChange = request.alertOnChange,
            email = request.email
        )
        // 상태 변경 로직은 별도 API로 분리하거나 여기에 포함 (예: status 필드 추가)
        val updatedCrawler = crawlerRepository.save(crawler)
        logger.info("Crawler updated: id=${updatedCrawler.id}")
        return CrawlerResponse.fromEntity(updatedCrawler)
    }

    @Transactional
    fun deleteCrawler(id: UUID) {
        if (!crawlerRepository.existsById(id)) {
            throw CrawlerNotFoundException(id)
        }
        // 연관된 로그도 삭제할지 정책 결정 필요 (여기서는 크롤러만 삭제)
        crawlerRepository.deleteById(id)
        logger.info("Crawler deleted: id=$id")
    }

    @Transactional
    fun checkCrawlerImmediately(id: UUID): SimpleMessageResponse {
        val crawler = crawlerRepository.findById(id)
            .orElseThrow { CrawlerNotFoundException(id) }

        logger.info("Immediately checking crawler: id=${crawler.id}, url=${crawler.url}")
        executeCrawlingAndNotification(crawler)
        return SimpleMessageResponse("Crawler id '$id' check initiated and processed.")
    }

    // 스케줄러가 호출하거나 즉시 실행 시 호출될 메서드
    @Transactional
    fun executeCrawlingAndNotification(crawler: Crawler) {
        var success = false
        var crawledValue: String? = null
        var errorMessage: String? = null
        var notificationSent = false

        try {
            crawledValue = webCrawlerService.crawl(crawler.url, crawler.selector)
            success = true
            logger.info("Crawled successfully: id=${crawler.id}, url=${crawler.url}, value=$crawledValue")

            val previousValue = crawler.lastCrawledValue
            crawler.updateCrawledData(crawledValue) // DB에 최신 값 및 시간 업데이트

            // 알림 조건 확인
            var shouldNotify = false
            val notificationReason = mutableListOf<String>()

            if (crawler.alertOnChange && previousValue != crawledValue) {
                shouldNotify = true
                notificationReason.add("내용 변경됨 (이전: '$previousValue', 현재: '$crawledValue')")
            }
            if (!crawler.alertKeyword.isNullOrBlank() && crawledValue?.contains(crawler.alertKeyword!!, ignoreCase = true) == true) {
                shouldNotify = true
                notificationReason.add("키워드 '${crawler.alertKeyword}' 포함됨")
            }

            if (shouldNotify) {
                val subject = "웹 알리미: ${crawler.url} 변경 감지"
                val textBody = """
                    안녕하세요.
                    요청하신 웹사이트 정보가 변경되어 알림을 드립니다.

                    URL: ${crawler.url}
                    CSS Selector: ${crawler.selector}
                    감지된 값: $crawledValue
                    변경 사유: ${notificationReason.joinToString(", ")}

                    확인 시간: ${LocalDateTime.now()}
                """.trimIndent()
                try {
                    notificationService.sendNotification(crawler.email, subject, textBody)
                    notificationSent = true
                    logger.info("Notification sent for crawler id: ${crawler.id} to ${crawler.email}")
                } catch (e: Exception) {
                    logger.error("Failed to send notification for crawler id: ${crawler.id}", e)
                    // 알림 실패에 대한 처리 (예: 재시도 로직은 여기서는 생략)
                }
            }
            crawler.status = CrawlerStatus.ACTIVE // 성공 시 ACTIVE로 (ERROR였다면)
        } catch (e: Exception) {
            errorMessage = e.message?.take(490) // DB 컬럼 길이 고려
            crawler.markAsError(errorMessage)
            logger.error("Error crawling crawler id: ${crawler.id}, url: ${crawler.url}", e)
        } finally {
            crawlerRepository.save(crawler) // 상태 및 값 최종 저장
            // 크롤링 로그 기록
            val crawlLog = CrawlLog(
                crawler = crawler,
                crawledValue = crawledValue,
                success = success,
                errorMessage = errorMessage,
                notificationSent = notificationSent
            )
            crawlLogRepository.save(crawlLog)
        }
    }

    @Transactional(readOnly = true)
    fun getCrawlerLogs(crawlerId: UUID): List<CrawlLogResponse> {
        if (!crawlerRepository.existsById(crawlerId)) {
            throw CrawlerNotFoundException(crawlerId)
        }
        // 최근 5개 로그만 반환하는 예시 (필요시 페이징 처리)
        return crawlLogRepository.findTop5ByCrawlerIdOrderByCrawledAtDesc(crawlerId).map {
            CrawlLogResponse(it.id, it.crawledAt, it.crawledValue, it.success, it.errorMessage, it.notificationSent)
        }
    }

    @Transactional
    fun activateCrawler(id: UUID): CrawlerResponse {
        val crawler = crawlerRepository.findById(id)
            .orElseThrow { CrawlerNotFoundException(id) }
        crawler.activate()
        val savedCrawler = crawlerRepository.save(crawler)
        logger.info("Crawler activated: id=${savedCrawler.id}")
        return CrawlerResponse.fromEntity(savedCrawler)
    }

    @Transactional
    fun deactivateCrawler(id: UUID): CrawlerResponse {
        val crawler = crawlerRepository.findById(id)
            .orElseThrow { CrawlerNotFoundException(id) }
        crawler.deactivate()
        val savedCrawler = crawlerRepository.save(crawler)
        logger.info("Crawler deactivated: id=${savedCrawler.id}")
        return CrawlerResponse.fromEntity(savedCrawler)
    }

}