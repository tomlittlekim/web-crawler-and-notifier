package kr.co.webcrawlerandnotifier.domain.model.crawler

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 크롤러 설정 정보를 담는 Value Object
 * 
 * Value Object 패턴을 적용하여 크롤링과 관련된 설정들을 하나의 객체로 묶었습니다.
 * 불변 객체로 설계되어 데이터의 무결성을 보장하며, 생성 시점에 유효성 검사를 수행합니다.
 * 
 * 장점:
 * - 관련된 데이터를 논리적으로 그룹화
 * - 불변성을 통한 안전성 보장
 * - 생성자에서 유효성 검사 수행
 * - 재사용 가능한 설정 객체
 */
@Embeddable
data class CrawlerConfiguration(
    /** 크롤링할 웹사이트의 URL */
    @Column(nullable = false)
    val url: String,
    
    /** 크롤링할 요소를 지정하는 CSS 선택자 (예: "div.content", "#title") */
    @Column(nullable = false, name = "css_selector")
    val selector: String,
    
    /** 크롤링 체크 간격 (밀리초 단위) */
    @Column(nullable = false, name = "check_interval_ms")
    val checkIntervalMs: Long,
    
    /** 알림을 발송할 키워드 (이 키워드가 포함되면 알림 발송) */
    val alertKeyword: String? = null,
    
    /** 내용 변경 시 알림 발송 여부 */
    val alertOnChange: Boolean = false
) {
    /**
     * 객체 생성 시 유효성 검사를 수행합니다.
     * 
     * 검사 항목:
     * - URL이 비어있지 않은지 확인
     * - CSS 선택자가 비어있지 않은지 확인  
     * - 체크 간격이 양수인지 확인
     */
    init {
        require(url.isNotBlank()) { "URL은 비어있을 수 없습니다" }
        require(selector.isNotBlank()) { "CSS 선택자는 비어있을 수 없습니다" }
        require(checkIntervalMs > 0) { "체크 간격은 0보다 커야 합니다" }
    }
    
    /**
     * 체크 간격을 분 단위로 반환합니다.
     * 
     * @return 체크 간격 (분)
     */
    fun getCheckIntervalInMinutes(): Long = checkIntervalMs / (1000 * 60)
    
    /**
     * 알림 조건이 설정되어 있는지 확인합니다.
     * 
     * @return 키워드 알림 또는 변경 감지 알림이 설정되어 있으면 true
     */
    fun hasAlertCondition(): Boolean = !alertKeyword.isNullOrBlank() || alertOnChange
} 