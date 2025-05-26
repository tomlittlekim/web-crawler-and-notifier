package kr.co.webcrawlerandnotifier.domain.service

import kr.co.webcrawlerandnotifier.domain.model.crawler.Crawler
import kr.co.webcrawlerandnotifier.domain.model.crawler.NotificationType
import kr.co.webcrawlerandnotifier.domain.exception.NotificationException
import kr.co.webcrawlerandnotifier.infrastructure.notification.NotificationService as InfraNotificationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 알림 발송 관련 도메인 서비스
 * 
 * 알림 발송과 관련된 비즈니스 로직을 담당합니다.
 * 인프라스트럭처 계층의 알림 서비스들을 조합하여 
 * 도메인 요구사항에 맞는 알림 발송을 처리합니다.
 * 
 * 주요 책임:
 * - 알림 타입에 따른 적절한 알림 서비스 선택
 * - 알림 발송 실패 처리
 * - 알림 발송 결과 추적
 */
@Service
class NotificationDomainService(
    @Qualifier("emailNotificationService") 
    private val emailNotificationService: InfraNotificationService,
    
    @Qualifier("slackNotificationService") 
    private val slackNotificationService: InfraNotificationService? = null
) {
    
    private val logger = LoggerFactory.getLogger(NotificationDomainService::class.java)
    
    /**
     * 크롤러 설정에 따라 적절한 알림을 발송합니다.
     * 
     * @param crawler 크롤러 엔티티
     * @param subject 알림 제목
     * @param message 알림 내용
     * @return 알림 발송 결과
     */
    fun sendNotification(
        crawler: Crawler, 
        subject: String, 
        message: String
    ): NotificationResult {
        val notificationConfig = crawler.notificationConfiguration
        val results = mutableListOf<SingleNotificationResult>()
        
        try {
            // 이메일 알림 발송
            if (notificationConfig.isEmailEnabled()) {
                val emailResult = sendEmailNotification(notificationConfig.email, subject, message)
                results.add(emailResult)
            }
            
            // Slack 알림 발송
            if (notificationConfig.isSlackEnabled()) {
                val slackResult = sendSlackNotification(notificationConfig.slackChannelId!!, subject, message)
                results.add(slackResult)
            }
            
            val overallSuccess = results.all { it.success }
            val errorMessages = results.filter { !it.success }.map { it.errorMessage }.filterNotNull()
            
            return NotificationResult(
                success = overallSuccess,
                emailSent = results.any { it.type == NotificationType.EMAIL && it.success },
                slackSent = results.any { it.type == NotificationType.SLACK && it.success },
                errorMessages = errorMessages,
                sentAt = LocalDateTime.now()
            )
            
        } catch (e: Exception) {
            logger.error("알림 발송 중 예상치 못한 오류 발생: crawler=${crawler.id}", e)
            throw NotificationException("알림 발송 실패", e)
        }
    }
    
    /**
     * 이메일 알림을 발송합니다.
     * 
     * @param email 수신자 이메일
     * @param subject 제목
     * @param message 내용
     * @return 발송 결과
     */
    private fun sendEmailNotification(
        email: String, 
        subject: String, 
        message: String
    ): SingleNotificationResult {
        return try {
            emailNotificationService.sendNotification(email, subject, message)
            logger.info("이메일 알림 발송 성공: $email")
            SingleNotificationResult(
                type = NotificationType.EMAIL,
                success = true,
                recipient = email
            )
        } catch (e: Exception) {
            logger.error("이메일 알림 발송 실패: $email", e)
            SingleNotificationResult(
                type = NotificationType.EMAIL,
                success = false,
                recipient = email,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Slack 알림을 발송합니다.
     * 
     * @param channelId Slack 채널 ID
     * @param subject 제목
     * @param message 내용
     * @return 발송 결과
     */
    private fun sendSlackNotification(
        channelId: String, 
        subject: String, 
        message: String
    ): SingleNotificationResult {
        return try {
            if (slackNotificationService == null) {
                throw NotificationException("Slack 알림 서비스가 설정되지 않았습니다")
            }
            
            val slackMessage = "$subject\n\n$message"
            slackNotificationService.sendNotification(channelId, subject, slackMessage)
            logger.info("Slack 알림 발송 성공: $channelId")
            SingleNotificationResult(
                type = NotificationType.SLACK,
                success = true,
                recipient = channelId
            )
        } catch (e: Exception) {
            logger.error("Slack 알림 발송 실패: $channelId", e)
            SingleNotificationResult(
                type = NotificationType.SLACK,
                success = false,
                recipient = channelId,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * 알림 메시지를 생성합니다.
     * 
     * @param crawler 크롤러 엔티티
     * @param crawledValue 크롤링된 값
     * @param reasons 알림 이유 목록
     * @return 생성된 알림 메시지
     */
    fun createNotificationMessage(
        crawler: Crawler, 
        crawledValue: String?, 
        reasons: List<String>
    ): NotificationMessage {
        val subject = "웹 알리미: ${crawler.configuration.url} 변경 감지"
        val textBody = """
            안녕하세요.
            요청하신 웹사이트 정보가 변경되어 알림을 드립니다.

            URL: ${crawler.configuration.url}
            CSS Selector: ${crawler.configuration.selector}
            감지된 값: $crawledValue
            변경 사유: ${reasons.joinToString(", ")}

            확인 시간: ${LocalDateTime.now()}
        """.trimIndent()
        
        return NotificationMessage(subject, textBody)
    }
}

/**
 * 알림 메시지 정보를 담는 데이터 클래스
 * 
 * @property subject 알림 제목
 * @property body 알림 내용
 */
data class NotificationMessage(
    val subject: String,
    val body: String
)

/**
 * 전체 알림 발송 결과를 담는 데이터 클래스
 * 
 * @property success 전체 발송 성공 여부
 * @property emailSent 이메일 발송 성공 여부
 * @property slackSent Slack 발송 성공 여부
 * @property errorMessages 오류 메시지 목록
 * @property sentAt 발송 시간
 */
data class NotificationResult(
    val success: Boolean,
    val emailSent: Boolean,
    val slackSent: Boolean,
    val errorMessages: List<String>,
    val sentAt: LocalDateTime
)

/**
 * 개별 알림 발송 결과를 담는 데이터 클래스
 * 
 * @property type 알림 타입
 * @property success 발송 성공 여부
 * @property recipient 수신자
 * @property errorMessage 오류 메시지
 */
data class SingleNotificationResult(
    val type: NotificationType,
    val success: Boolean,
    val recipient: String,
    val errorMessage: String? = null
) 