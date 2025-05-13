package kr.co.webcrawlerandnotifier.domain.repository

import kr.co.webcrawlerandnotifier.domain.model.crawler.Crawler
import kr.co.webcrawlerandnotifier.domain.model.crawler.CrawlerStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CrawlerRepository : JpaRepository<Crawler, UUID> {
    fun findAllByStatus(status: CrawlerStatus): List<Crawler>
}