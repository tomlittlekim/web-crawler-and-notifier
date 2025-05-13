package kr.co.webcrawlerandnotifier.domain.model.log

import jakarta.persistence.*
import kr.co.webcrawlerandnotifier.domain.model.crawler.Crawler
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crawl_logs")
data class CrawlLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawler_id", nullable = false)
    var crawler: Crawler,

    @Column(nullable = false)
    var crawledAt: LocalDateTime = LocalDateTime.now(),

    @Column(length = 1000)
    var crawledValue: String?,

    var success: Boolean,

    @Column(length = 500)
    var errorMessage: String? = null,

    var notificationSent: Boolean = false
)