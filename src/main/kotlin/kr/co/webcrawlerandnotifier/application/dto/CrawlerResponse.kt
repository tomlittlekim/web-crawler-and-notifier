package kr.co.webcrawlerandnotifier.application.dto

import kr.co.webcrawlerandnotifier.domain.model.crawler.Crawler
import kr.co.webcrawlerandnotifier.domain.model.crawler.CrawlerStatus
import kr.co.webcrawlerandnotifier.domain.model.crawler.NotificationType
import java.time.LocalDateTime
import java.util.UUID

data class CrawlerResponse(
    val id: UUID?,
    val url: String,
    val selector: String,
    val checkInterval: Long,
    val alertKeyword: String?,
    val alertOnChange: Boolean,
    val email: String?,
    val notificationType: NotificationType,
    val slackChannelId: String?,
    val status: CrawlerStatus,
    val lastCrawledValue: String?,
    val lastCheckedAt: LocalDateTime?,
    val lastChangedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(crawler: Crawler): CrawlerResponse {
            return CrawlerResponse(
                id = crawler.id,
                url = crawler.url,
                selector = crawler.selector,
                checkInterval = crawler.checkIntervalMs,
                alertKeyword = crawler.alertKeyword,
                alertOnChange = crawler.alertOnChange,
                email = crawler.email,
                notificationType = crawler.notificationType,
                slackChannelId = crawler.slackChannelId,
                status = crawler.status,
                lastCrawledValue = crawler.lastCrawledValue,
                lastCheckedAt = crawler.lastCheckedAt,
                lastChangedAt = crawler.lastChangedAt,
                createdAt = crawler.createdAt,
                updatedAt = crawler.updatedAt
            )
        }
    }
}

data class SimpleMessageResponse(val message: String)

data class CrawlLogResponse(
    val id: UUID?,
    val crawledAt: LocalDateTime,
    val crawledValue: String?,
    val success: Boolean,
    val errorMessage: String?,
    val notificationSent: Boolean
)