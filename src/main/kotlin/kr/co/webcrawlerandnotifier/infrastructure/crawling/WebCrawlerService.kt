package kr.co.webcrawlerandnotifier.infrastructure.crawling

import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component // 인터페이스로 만들고 구현체로 분리하는 것이 DDD에 더 적합
class WebCrawlerService {
    fun crawl(url: String, selector: String): String? {
        try {
            val doc = Jsoup.connect(url)
                .timeout(10000) // 10초 타임아웃
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // User-Agent 설정
                .get()
            val elements = doc.select(selector)
            return if (elements.isNotEmpty()) elements.first()?.text()?.trim() else null
        } catch (e: Exception) {
            // 로깅은 호출하는 쪽에서 처리하거나 여기서 기본 로깅
            throw RuntimeException("Failed to crawl url: $url with selector: $selector. Error: ${e.message}", e)
        }
    }
}