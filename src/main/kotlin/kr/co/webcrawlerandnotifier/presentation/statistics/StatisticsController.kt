package kr.co.webcrawlerandnotifier.presentation.statistics

import kr.co.webcrawlerandnotifier.application.dto.statistics.*
import kr.co.webcrawlerandnotifier.application.statistics.StatisticsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping("/overall-crawl")
    fun getOverallCrawlStats(): ResponseEntity<OverallStatsDto> {
        return ResponseEntity.ok(statisticsService.getOverallCrawlStats())
    }

    @GetMapping("/event-type-summary")
    fun getEventTypeSummaries(): ResponseEntity<List<EventTypeSummaryDto>> {
        return ResponseEntity.ok(statisticsService.getEventTypeSummaries())
    }

    @GetMapping("/recent-errors")
    fun getRecentErrors(@RequestParam(defaultValue = "10") limit: Int): ResponseEntity<List<RecentErrorDto>> {
        return ResponseEntity.ok(statisticsService.getRecentErrors(limit))
    }

    @GetMapping("/url-crawl-stats")
    fun getUrlCrawlStats(): ResponseEntity<List<UrlCrawlStatsDto>> {
        return ResponseEntity.ok(statisticsService.getUrlCrawlStats())
    }
} 