package kr.co.webcrawlerandnotifier.domain.model.crawler

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 웹 크롤러 엔티티 - 리팩토링된 버전
 * 
 * Value Object 패턴을 적용하여 관심사를 분리하고 도메인 로직을 캡슐화했습니다.
 * - CrawlerConfiguration: 크롤링 설정 정보
 * - NotificationConfiguration: 알림 설정 정보  
 * - CrawlerState: 크롤러 상태 정보
 * 
 * 이렇게 분리함으로써 각 영역의 책임이 명확해지고 유지보수가 용이해집니다.
 */
@Entity
@Table(name = "crawlers")
data class Crawler(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    /**
     * 크롤링 관련 설정을 담는 Value Object
     * URL, CSS 선택자, 체크 간격, 알림 조건 등을 포함
     */
    @Embedded
    var configuration: CrawlerConfiguration,

    /**
     * 알림 관련 설정을 담는 Value Object
     * 이메일, 알림 타입, Slack 채널 등을 포함
     */
    @Embedded
    var notificationConfiguration: NotificationConfiguration,

    /**
     * 크롤러의 현재 상태를 담는 Value Object
     * 상태, 마지막 크롤링 값, 체크 시간 등을 포함
     */
    @Embedded
    var state: CrawlerState = CrawlerState(),

    /** 생성 시간 (변경 불가) */
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    /** 마지막 수정 시간 */
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    
    /**
     * 크롤링 설정 정보를 업데이트합니다.
     * 
     * @param newConfiguration 새로운 크롤링 설정
     */
    fun updateConfiguration(newConfiguration: CrawlerConfiguration) {
        this.configuration = newConfiguration
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 알림 설정 정보를 업데이트합니다.
     * 
     * @param newNotificationConfiguration 새로운 알림 설정
     */
    fun updateNotificationConfiguration(newNotificationConfiguration: NotificationConfiguration) {
        this.notificationConfiguration = newNotificationConfiguration
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 크롤링된 데이터를 업데이트합니다.
     * 값이 변경된 경우에만 변경 시간을 업데이트합니다.
     * 
     * @param newValue 새로 크롤링된 값
     */
    fun updateCrawledData(newValue: String?) {
        this.state = state.withUpdatedValue(newValue)
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 크롤러를 활성화 상태로 변경합니다.
     */
    fun activate() {
        this.state = state.withStatus(CrawlerStatus.ACTIVE)
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 크롤러를 비활성화 상태로 변경합니다.
     */
    fun deactivate() {
        this.state = state.withStatus(CrawlerStatus.INACTIVE)
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 크롤러를 에러 상태로 변경하고 에러 메시지를 저장합니다.
     * 
     * @param errorMessage 에러 메시지
     */
    fun markAsError(errorMessage: String) {
        this.state = state.withError(errorMessage)
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 주어진 값에 대해 알림을 보내야 하는지 판단합니다.
     * 
     * @param newValue 새로 크롤링된 값
     * @return 알림 발송 여부
     */
    fun shouldNotify(newValue: String?): Boolean {
        // 키워드가 포함된 경우 알림
        val hasKeyword = !configuration.alertKeyword.isNullOrBlank() && 
                        newValue?.contains(configuration.alertKeyword!!, ignoreCase = true) == true
        
        // 내용이 변경된 경우 알림 (변경 감지 옵션이 켜져있을 때)
        val hasChanged = configuration.alertOnChange && state.lastCrawledValue != newValue
        
        return hasKeyword || hasChanged
    }

    /**
     * 알림을 보내는 이유를 문자열 리스트로 반환합니다.
     * 
     * @param newValue 새로 크롤링된 값
     * @return 알림 이유 목록
     */
    fun getNotificationReasons(newValue: String?): List<String> {
        val reasons = mutableListOf<String>()
        
        // 내용 변경 감지
        if (configuration.alertOnChange && state.lastCrawledValue != newValue) {
            reasons.add("내용 변경됨 (이전: '${state.lastCrawledValue?.take(50)}', 현재: '${newValue?.take(50)}')")
        }
        
        // 키워드 포함 감지
        if (!configuration.alertKeyword.isNullOrBlank() && 
            newValue?.contains(configuration.alertKeyword!!, ignoreCase = true) == true) {
            reasons.add("키워드 '${configuration.alertKeyword}' 포함됨")
        }
        
        return reasons
    }

    // 기존 코드와의 호환성을 위한 프로퍼티들 (Deprecated)
    @Deprecated("configuration.url을 사용하세요", ReplaceWith("configuration.url"))
    val url: String get() = configuration.url
    
    @Deprecated("configuration.selector를 사용하세요", ReplaceWith("configuration.selector"))
    val selector: String get() = configuration.selector
    
    @Deprecated("configuration.checkIntervalMs를 사용하세요", ReplaceWith("configuration.checkIntervalMs"))
    val checkIntervalMs: Long get() = configuration.checkIntervalMs
    
    @Deprecated("configuration.alertKeyword를 사용하세요", ReplaceWith("configuration.alertKeyword"))
    val alertKeyword: String? get() = configuration.alertKeyword
    
    @Deprecated("configuration.alertOnChange를 사용하세요", ReplaceWith("configuration.alertOnChange"))
    val alertOnChange: Boolean get() = configuration.alertOnChange
    
    @Deprecated("notificationConfiguration.email을 사용하세요", ReplaceWith("notificationConfiguration.email"))
    val email: String get() = notificationConfiguration.email
    
    @Deprecated("notificationConfiguration.notificationType을 사용하세요", ReplaceWith("notificationConfiguration.notificationType"))
    val notificationType: NotificationType get() = notificationConfiguration.notificationType
    
    @Deprecated("notificationConfiguration.slackChannelId를 사용하세요", ReplaceWith("notificationConfiguration.slackChannelId"))
    val slackChannelId: String? get() = notificationConfiguration.slackChannelId
    
    @Deprecated("state.status를 사용하세요", ReplaceWith("state.status"))
    val status: CrawlerStatus get() = state.status
    
    @Deprecated("state.lastCrawledValue를 사용하세요", ReplaceWith("state.lastCrawledValue"))
    val lastCrawledValue: String? get() = state.lastCrawledValue
    
    @Deprecated("state.lastCheckedAt를 사용하세요", ReplaceWith("state.lastCheckedAt"))
    val lastCheckedAt: LocalDateTime? get() = state.lastCheckedAt
    
    @Deprecated("state.lastChangedAt를 사용하세요", ReplaceWith("state.lastChangedAt"))
    val lastChangedAt: LocalDateTime? get() = state.lastChangedAt
}