package kr.co.webcrawlerandnotifier.domain.model.crawler

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.LocalDateTime
import java.time.Duration

/**
 * 크롤러 상태 정보를 담는 Value Object
 * 
 * 크롤러의 현재 상태와 크롤링 이력을 관리합니다.
 * 불변 객체로 설계되어 상태 변경 시 새로운 객체를 반환하는 방식을 사용합니다.
 * 
 * 이 패턴의 장점:
 * - 상태 변경 추적이 용이
 * - 동시성 문제 방지
 * - 예측 가능한 상태 관리
 */
@Embeddable
data class CrawlerState(
    /** 크롤러의 현재 상태 (PENDING, ACTIVE, INACTIVE, ERROR) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: CrawlerStatus = CrawlerStatus.PENDING,
    
    /** 마지막으로 크롤링된 값 (최대 1000자) */
    @Column(length = 1000)
    val lastCrawledValue: String? = null,
    
    /** 마지막으로 크롤링을 시도한 시간 */
    val lastCheckedAt: LocalDateTime? = null,
    
    /** 마지막으로 내용이 변경된 시간 */
    val lastChangedAt: LocalDateTime? = null
) {
    /**
     * 크롤링된 값을 업데이트하고 새로운 상태 객체를 반환합니다.
     * 
     * 동작 방식:
     * - 값이 변경된 경우: lastCheckedAt과 lastChangedAt 모두 업데이트
     * - 값이 동일한 경우: lastCheckedAt만 업데이트
     * 
     * @param newValue 새로 크롤링된 값
     * @return 업데이트된 새로운 CrawlerState 객체
     */
    fun withUpdatedValue(newValue: String?): CrawlerState {
        val now = LocalDateTime.now()
        return if (this.lastCrawledValue != newValue) {
            // 값이 변경된 경우 - 변경 시간도 업데이트
            copy(
                lastCrawledValue = newValue,
                lastCheckedAt = now,
                lastChangedAt = now
            )
        } else {
            // 값이 동일한 경우 - 체크 시간만 업데이트
            copy(lastCheckedAt = now)
        }
    }
    
    /**
     * 크롤러 상태를 변경하고 새로운 상태 객체를 반환합니다.
     * 
     * @param newStatus 새로운 상태
     * @return 상태가 변경된 새로운 CrawlerState 객체
     */
    fun withStatus(newStatus: CrawlerStatus): CrawlerState {
        return copy(status = newStatus)
    }
    
    /**
     * 크롤러를 에러 상태로 변경하고 에러 메시지를 저장합니다.
     * 
     * @param errorMessage 에러 메시지
     * @return 에러 상태로 변경된 새로운 CrawlerState 객체
     */
    fun withError(errorMessage: String): CrawlerState {
        return copy(
            status = CrawlerStatus.ERROR,
            lastCrawledValue = "Error: $errorMessage"
        )
    }
    
    /**
     * 크롤러가 활성 상태인지 확인합니다.
     * 
     * @return ACTIVE 상태인 경우 true
     */
    fun isActive(): Boolean = status == CrawlerStatus.ACTIVE
    
    /**
     * 크롤러가 에러 상태인지 확인합니다.
     * 
     * @return ERROR 상태인 경우 true
     */
    fun isError(): Boolean = status == CrawlerStatus.ERROR
    
    /**
     * 마지막 체크로부터 경과된 시간을 반환합니다.
     * 
     * @return 경과 시간 (Duration), 체크한 적이 없으면 null
     */
    fun getTimeSinceLastCheck(): Duration? {
        return lastCheckedAt?.let { Duration.between(it, LocalDateTime.now()) }
    }
    
    /**
     * 마지막 변경으로부터 경과된 시간을 반환합니다.
     * 
     * @return 경과 시간 (Duration), 변경된 적이 없으면 null
     */
    fun getTimeSinceLastChange(): Duration? {
        return lastChangedAt?.let { Duration.between(it, LocalDateTime.now()) }
    }
    
    /**
     * 크롤링된 값이 있는지 확인합니다.
     * 
     * @return 크롤링된 값이 있고 에러가 아닌 경우 true
     */
    fun hasValidValue(): Boolean = !lastCrawledValue.isNullOrBlank() && !lastCrawledValue!!.startsWith("Error:")
} 