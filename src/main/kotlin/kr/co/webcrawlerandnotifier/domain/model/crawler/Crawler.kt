package kr.co.webcrawlerandnotifier.domain.model.crawler

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crawlers")
data class Crawler(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var url: String,

    @Column(nullable = false, name = "css_selector")
    var selector: String,

    @Column(nullable = false, name = "check_interval_ms")
    var checkIntervalMs: Long, // 밀리초 단위

    var alertKeyword: String? = null,

    var alertOnChange: Boolean = false,

    @Column(nullable = false)
    var email: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CrawlerStatus = CrawlerStatus.PENDING,

    @Column(length = 1000) // 크롤링된 값의 길이를 고려
    var lastCrawledValue: String? = null,

    var lastCheckedAt: LocalDateTime? = null,
    var lastChangedAt: LocalDateTime? = null, // 내용이 마지막으로 변경된 시간

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun updateDetails(
        url: String,
        selector: String,
        checkIntervalMs: Long,
        alertKeyword: String?,
        alertOnChange: Boolean,
        email: String
    ) {
        this.url = url
        this.selector = selector
        this.checkIntervalMs = checkIntervalMs
        this.alertKeyword = alertKeyword
        this.alertOnChange = alertOnChange
        this.email = email
        this.updatedAt = LocalDateTime.now()
    }

    fun updateCrawledData(newValue: String?) {
        this.lastCheckedAt = LocalDateTime.now()
        if (this.lastCrawledValue != newValue) {
            this.lastChangedAt = LocalDateTime.now()
            this.lastCrawledValue = newValue
        }
        this.updatedAt = LocalDateTime.now()
    }

    fun activate() {
        this.status = CrawlerStatus.ACTIVE
        this.updatedAt = LocalDateTime.now()
    }

    fun deactivate() {
        this.status = CrawlerStatus.INACTIVE
        this.updatedAt = LocalDateTime.now()
    }

    fun markAsError(errorMessage: String? = null) { // 간단한 에러 메시지 저장 (필요시 확장)
        this.status = CrawlerStatus.ERROR
        this.lastCrawledValue = "Error: ${errorMessage ?: "Unknown"}" // 에러 메시지를 값으로 저장
        this.updatedAt = LocalDateTime.now()
    }
}