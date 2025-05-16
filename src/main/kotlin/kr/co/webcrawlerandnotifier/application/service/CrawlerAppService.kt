package kr.co.webcrawlerandnotifier.application.service

import kr.co.webcrawlerandnotifier.application.dto.*
import kr.co.webcrawlerandnotifier.domain.exception.CrawlerNotFoundException
import kr.co.webcrawlerandnotifier.domain.model.crawler.Crawler
import kr.co.webcrawlerandnotifier.domain.model.crawler.CrawlerStatus
import kr.co.webcrawlerandnotifier.domain.model.crawler.NotificationType
import kr.co.webcrawlerandnotifier.domain.model.log.CrawlLog
import kr.co.webcrawlerandnotifier.domain.repository.CrawlLogRepository
import kr.co.webcrawlerandnotifier.domain.repository.CrawlerRepository
import kr.co.webcrawlerandnotifier.infrastructure.crawling.WebCrawlerService
import kr.co.webcrawlerandnotifier.infrastructure.notification.NotificationService
import kr.co.webcrawlerandnotifier.application.statistics.StatisticsService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class CrawlerAppService(
    private val crawlerRepository: CrawlerRepository,
    private val crawlLogRepository: CrawlLogRepository,
    private val webCrawlerService: WebCrawlerService,
    @Qualifier("emailNotificationService") private val emailNotificationService: NotificationService,
    @Qualifier("slackNotificationService") private val slackNotificationService: NotificationService? = null,
    private val statisticsService: StatisticsService
) {
    private val logger = LoggerFactory.getLogger(CrawlerAppService::class.java)

    @Transactional
    fun createCrawler(request: CreateCrawlerRequest): CrawlerResponse {
        validateNotificationRequest(request.notificationType, request.email, request.slackChannelId)

        val crawler = Crawler(
            url = request.url,
            selector = request.selector,
            checkIntervalMs = request.checkInterval,
            alertKeyword = request.alertKeyword,
            alertOnChange = request.alertOnChange,
            email = request.email ?: "",
            notificationType = request.notificationType,
            slackChannelId = request.slackChannelId,
            status = CrawlerStatus.ACTIVE
        )
        val savedCrawler = crawlerRepository.save(crawler)
        logger.info("Crawler created: id=${savedCrawler.id}, url=${savedCrawler.url}, notificationType=${savedCrawler.notificationType}")
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
        validateNotificationRequest(request.notificationType, request.email, request.slackChannelId)

        val crawler = crawlerRepository.findById(id)
            .orElseThrow { CrawlerNotFoundException(id) }

        crawler.updateDetails(
            url = request.url,
            selector = request.selector,
            checkIntervalMs = request.checkInterval,
            alertKeyword = request.alertKeyword,
            alertOnChange = request.alertOnChange,
            email = request.email ?: "",
            notificationType = request.notificationType,
            slackChannelId = request.slackChannelId
        )
        val updatedCrawler = crawlerRepository.save(crawler)
        logger.info("Crawler updated: id=${updatedCrawler.id}, notificationType=${updatedCrawler.notificationType}")
        return CrawlerResponse.fromEntity(updatedCrawler)
    }

    private fun validateNotificationRequest(notificationType: NotificationType, email: String?, slackChannelId: String?) {
        when (notificationType) {
            NotificationType.EMAIL -> {
                if (email.isNullOrBlank()) {
                    throw IllegalArgumentException("Email must be provided for EMAIL notification type.")
                }
            }
            NotificationType.SLACK -> {
                if (slackChannelId.isNullOrBlank()) {
                    throw IllegalArgumentException("Slack channel ID must be provided for SLACK notification type.")
                }
            }
            NotificationType.BOTH -> {
                if (email.isNullOrBlank()) {
                    throw IllegalArgumentException("Email must be provided for BOTH notification type.")
                }
                if (slackChannelId.isNullOrBlank()) {
                    throw IllegalArgumentException("Slack channel ID must be provided for BOTH notification type.")
                }
            }
        }
    }

    @Transactional
    fun deleteCrawler(id: UUID) {
        if (!crawlerRepository.existsById(id)) {
            throw CrawlerNotFoundException(id)
        }
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

    @Transactional
    fun executeCrawlingAndNotification(crawler: Crawler) {
        var success = false
        var crawledValue: String? = null
        var errorMessage: String? = null
        var notificationSent = false
        val startTime = Instant.now()
        var durationMsCrawl: Long = 0

        try {
            crawledValue = webCrawlerService.crawl(crawler.url, crawler.selector)
            success = true
            durationMsCrawl = Duration.between(startTime, Instant.now()).toMillis()
            logger.info("Crawled successfully: id=${crawler.id}, url=${crawler.url}, value=$crawledValue, durationMs=$durationMsCrawl")

            statisticsService.recordEvent(
                eventType = "CRAWL_ATTEMPT",
                targetUrl = crawler.url,
                durationMs = durationMsCrawl,
                isSuccess = true,
                details = "Crawled value: ${crawledValue?.take(200)}"
            )

            val previousValue = crawler.lastCrawledValue
            crawler.updateCrawledData(crawledValue)

            var shouldNotify = false
            val notificationReason = mutableListOf<String>()

            if (crawler.alertOnChange && previousValue != crawledValue) {
                shouldNotify = true
                notificationReason.add("내용 변경됨 (이전: '${previousValue?.take(50)}', 현재: '${crawledValue?.take(50)}')")
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

                var emailSent = false
                var slackSent = false

                if (crawler.notificationType == NotificationType.EMAIL || crawler.notificationType == NotificationType.BOTH) {
                    if (crawler.email.isNotBlank()) {
                        val emailNotificationStartTime = Instant.now()
                        var emailSuccess = false
                        var emailErrorDetails: String? = null
                        try {
                            emailNotificationService.sendNotification(crawler.email, subject, textBody)
                            emailSent = true
                            emailSuccess = true
                            logger.info("Email notification sent for crawler id: ${crawler.id} to ${crawler.email}")
                        } catch (e: Exception) {
                            logger.error("Failed to send email notification for crawler id: ${crawler.id}", e)
                            emailErrorDetails = e.message?.take(200)
                        }
                        val durationMsEmail = Duration.between(emailNotificationStartTime, Instant.now()).toMillis()
                        statisticsService.recordEvent(
                            eventType = "EMAIL_NOTIFICATION_ATTEMPT",
                            targetUrl = crawler.email,
                            durationMs = durationMsEmail,
                            isSuccess = emailSuccess,
                            details = if(emailSuccess) "Subject: ${subject.take(100)}" else "Error: $emailErrorDetails"
                        )
                    } else {
                        logger.warn("Email address is blank for crawler id: ${crawler.id}, notification type: ${crawler.notificationType}. Skipping email notification.")
                    }
                }

                if (crawler.notificationType == NotificationType.SLACK || crawler.notificationType == NotificationType.BOTH) {
                    if (slackNotificationService == null) {
                        logger.warn("SlackNotificationService is not available (slack.bot.token may not be configured). Skipping Slack notification for crawler id: ${crawler.id}")
                        statisticsService.recordEvent(
                            eventType = "SLACK_NOTIFICATION_ATTEMPT",
                            targetUrl = crawler.slackChannelId,
                            durationMs = 0,
                            isSuccess = false,
                            details = "SlackNotificationService not available (token missing?)"
                        )
                    } else if (!crawler.slackChannelId.isNullOrBlank()) {
                        val slackNotificationStartTime = Instant.now()
                        val slackMessageBody = """
                            URL: `${crawler.url}`
                            CSS Selector: `${crawler.selector}`
                            감지된 값: `$crawledValue`
                            변경 사유: `${notificationReason.joinToString(", ")}`
                            확인 시간: `${LocalDateTime.now()}`
                        """.trimIndent()
                        var slackSuccess = false
                        var slackErrorDetails: String? = null
                        try {
                            slackNotificationService.sendNotification(crawler.slackChannelId!!, subject, slackMessageBody)
                            slackSent = true
                            slackSuccess = true
                            logger.info("Slack notification sent for crawler id: ${crawler.id} to channel ${crawler.slackChannelId}")
                        } catch (e: Exception) {
                            logger.error("Failed to send Slack notification for crawler id: ${crawler.id}", e)
                            slackErrorDetails = e.message?.take(200)
                        }
                        val durationMsSlack = Duration.between(slackNotificationStartTime, Instant.now()).toMillis()
                        statisticsService.recordEvent(
                            eventType = "SLACK_NOTIFICATION_ATTEMPT",
                            targetUrl = crawler.slackChannelId,
                            durationMs = durationMsSlack,
                            isSuccess = slackSuccess,
                            details = if(slackSuccess) "Message sent" else "Error: $slackErrorDetails"
                        )
                    } else {
                        logger.warn("Slack channel ID is blank for crawler id: ${crawler.id}, notification type: ${crawler.notificationType}. Skipping Slack notification.")
                        statisticsService.recordEvent(
                            eventType = "SLACK_NOTIFICATION_ATTEMPT",
                            targetUrl = crawler.slackChannelId,
                            durationMs = 0,
                            isSuccess = false,
                            details = "Slack channel ID is blank"
                        )
                    }
                }
                notificationSent = emailSent || slackSent
            }
            crawler.status = CrawlerStatus.ACTIVE
        } catch (e: Exception) {
            success = false
            errorMessage = e.message?.take(490)
            durationMsCrawl = Duration.between(startTime, Instant.now()).toMillis()
            crawler.markAsError(errorMessage)
            logger.error("Error crawling crawler id: ${crawler.id}, url: ${crawler.url}, durationMs=$durationMsCrawl", e)
        
            statisticsService.recordEvent(
                eventType = "CRAWL_ATTEMPT",
                targetUrl = crawler.url,
                durationMs = durationMsCrawl,
                isSuccess = false,
                details = "Error: $errorMessage"
            )
        } finally {
            val crawlLog = CrawlLog(
                crawler = crawler,
                crawledValue = crawledValue,
                success = success,
                errorMessage = errorMessage,
                notificationSent = notificationSent
            )
            crawlLogRepository.save(crawlLog)
            crawlerRepository.save(crawler)
        }
    }

    @Transactional(readOnly = true)
    fun getCrawlerLogs(crawlerId: UUID): List<CrawlLogResponse> {
        if (!crawlerRepository.existsById(crawlerId)) {
            throw CrawlerNotFoundException(crawlerId)
        }
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