package kr.co.webcrawlerandnotifier.domain.statistics

import org.springframework.data.domain.Pageable

interface StatisticsRepositoryCustom {
    fun getEventTypeSummariesWithQueryDsl(): List<EventTypeProjection>
    fun findRecentErrorsWithQueryDsl(pageable: Pageable): List<Statistics>
    fun getUrlCrawlStatsWithQueryDsl(): List<UrlCrawlStatProjection>
} 