package kr.co.webcrawlerandnotifier.domain.model.crawler

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * 알림 설정 정보를 담는 Value Object
 * 
 * 알림과 관련된 모든 설정을 하나의 객체로 관리합니다.
 * 알림 타입에 따라 필요한 정보가 다르므로, 생성 시점에 유효성 검사를 통해
 * 일관성을 보장합니다.
 * 
 * 지원하는 알림 타입:
 * - EMAIL: 이메일만 발송
 * - SLACK: Slack만 발송  
 * - BOTH: 이메일과 Slack 모두 발송
 */
@Embeddable
data class NotificationConfiguration(
    /** 알림을 받을 이메일 주소 */
    @Column(nullable = false)
    val email: String,
    
    /** 알림 타입 (EMAIL, SLACK, BOTH) */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    val notificationType: NotificationType = NotificationType.EMAIL,
    
    /** Slack 알림을 받을 채널 ID (예: "#general", "@username") */
    val slackChannelId: String? = null
) {
    /**
     * 알림 타입에 따른 유효성 검사를 수행합니다.
     * 
     * 검사 규칙:
     * - EMAIL 타입: 이메일 주소 필수
     * - SLACK 타입: Slack 채널 ID 필수
     * - BOTH 타입: 이메일 주소와 Slack 채널 ID 모두 필수
     */
    init {
        when (notificationType) {
            NotificationType.EMAIL -> {
                require(email.isNotBlank()) { "이메일 알림을 위해서는 이메일 주소가 필요합니다" }
            }
            NotificationType.SLACK -> {
                require(!slackChannelId.isNullOrBlank()) { "Slack 알림을 위해서는 채널 ID가 필요합니다" }
            }
            NotificationType.BOTH -> {
                require(email.isNotBlank()) { "이메일 알림을 위해서는 이메일 주소가 필요합니다" }
                require(!slackChannelId.isNullOrBlank()) { "Slack 알림을 위해서는 채널 ID가 필요합니다" }
            }
        }
    }
    
    /**
     * 이메일 알림이 활성화되어 있는지 확인합니다.
     * 
     * @return EMAIL 또는 BOTH 타입인 경우 true
     */
    fun isEmailEnabled(): Boolean = notificationType == NotificationType.EMAIL || notificationType == NotificationType.BOTH
    
    /**
     * Slack 알림이 활성화되어 있는지 확인합니다.
     * 
     * @return SLACK 또는 BOTH 타입인 경우 true
     */
    fun isSlackEnabled(): Boolean = notificationType == NotificationType.SLACK || notificationType == NotificationType.BOTH
    
    /**
     * 유효한 이메일 주소 형식인지 간단히 검사합니다.
     * 
     * @return 이메일 형식이 유효하면 true
     */
    fun hasValidEmailFormat(): Boolean = email.contains("@") && email.contains(".")
} 