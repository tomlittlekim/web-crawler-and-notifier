package kr.co.webcrawlerandnotifier.domain.repository

import kr.co.webcrawlerandnotifier.domain.model.log.CrawlLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CrawlLogRepository : JpaRepository<CrawlLog, UUID> {
    fun findTop5ByCrawlerIdOrderByCrawledAtDesc(crawlerId: UUID): List<CrawlLog> // 예시: 최근 5개 로그
}