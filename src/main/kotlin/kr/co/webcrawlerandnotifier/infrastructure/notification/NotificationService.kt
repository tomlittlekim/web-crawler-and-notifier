package kr.co.webcrawlerandnotifier.infrastructure.notification

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service // 인터페이스로 만들고 구현체로 분리하는 것이 DDD에 더 적합
class EmailNotificationService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}") private val senderEmail: String
) : NotificationService { // 아래 NotificationService 인터페이스 구현

    override fun sendNotification(to: String, subject: String, body: String) {
        try {
            val message = SimpleMailMessage()
            message.setFrom(senderEmail) // application.properties의 spring.mail.username과 동일해야 함
            message.setTo(to)
            message.setSubject(subject)
            message.setText(body)
            mailSender.send(message)
        } catch (e: Exception) {
            // 로깅은 호출하는 쪽에서 처리하거나 여기서 기본 로깅
            throw RuntimeException("Failed to send email to $to. Error: ${e.message}", e)
        }
    }
}

// 별도 파일로 분리 가능 (예: domain/service/NotificationService.kt)
interface NotificationService {
    fun sendNotification(to: String, subject: String, body: String)
}