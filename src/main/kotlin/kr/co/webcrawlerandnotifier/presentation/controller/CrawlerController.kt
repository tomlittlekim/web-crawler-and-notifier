package kr.co.webcrawlerandnotifier.presentation.controller

import jakarta.validation.Valid
import kr.co.webcrawlerandnotifier.application.dto.*
import kr.co.webcrawlerandnotifier.application.service.CrawlerAppService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/crawlers")
class CrawlerController(private val crawlerAppService: CrawlerAppService) {

    @PostMapping
    fun createCrawler(@Valid @RequestBody request: CreateCrawlerRequest): ResponseEntity<CrawlerResponse> {
        val crawlerResponse = crawlerAppService.createCrawler(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(crawlerResponse)
    }

    @GetMapping
    fun getAllCrawlers(): ResponseEntity<List<CrawlerResponse>> {
        return ResponseEntity.ok(crawlerAppService.getAllCrawlers())
    }

    @GetMapping("/{id}")
    fun getCrawlerById(@PathVariable id: UUID): ResponseEntity<CrawlerResponse> {
        return ResponseEntity.ok(crawlerAppService.getCrawlerById(id))
    }

    @PutMapping("/{id}")
    fun updateCrawler(@PathVariable id: UUID, @Valid @RequestBody request: UpdateCrawlerRequest): ResponseEntity<CrawlerResponse> {
        val updatedCrawler = crawlerAppService.updateCrawler(id, request)
        return ResponseEntity.ok(updatedCrawler)
    }

    @DeleteMapping("/{id}")
    fun deleteCrawler(@PathVariable id: UUID): ResponseEntity<SimpleMessageResponse> {
        crawlerAppService.deleteCrawler(id)
        return ResponseEntity.ok(SimpleMessageResponse("Crawler with id $id deleted successfully."))
    }

    @PostMapping("/{id}/check")
    fun checkCrawlerImmediately(@PathVariable id: UUID): ResponseEntity<SimpleMessageResponse> {
        val response = crawlerAppService.checkCrawlerImmediately(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/logs")
    fun getCrawlerLogs(@PathVariable id: UUID): ResponseEntity<List<CrawlLogResponse>> {
        return ResponseEntity.ok(crawlerAppService.getCrawlerLogs(id))
    }

    @PutMapping("/{id}/activate")
    fun activateCrawler(@PathVariable id: UUID): ResponseEntity<CrawlerResponse> {
        val crawlerResponse = crawlerAppService.activateCrawler(id)
        return ResponseEntity.ok(crawlerResponse)
    }

    @PutMapping("/{id}/deactivate")
    fun deactivateCrawler(@PathVariable id: UUID): ResponseEntity<CrawlerResponse> {
        val crawlerResponse = crawlerAppService.deactivateCrawler(id)
        return ResponseEntity.ok(crawlerResponse)
    }
}