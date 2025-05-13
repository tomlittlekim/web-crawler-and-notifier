package kr.co.webcrawlerandnotifier.domain.model.crawler

enum class CrawlerStatus {
    PENDING, // 등록 후 아직 실행되지 않음
    ACTIVE,  // 활성 (스케줄링 대상)
    INACTIVE, // 비활성 (사용자 중지)
    ERROR    // 크롤링 중 오류 발생
}