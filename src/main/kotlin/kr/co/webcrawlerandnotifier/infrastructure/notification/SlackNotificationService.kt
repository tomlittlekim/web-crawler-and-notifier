package kr.co.webcrawlerandnotifier.infrastructure.notification

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service("slackNotificationService")
@ConditionalOnProperty(name = ["slack.bot.token"], matchIfMissing = false)
class SlackNotificationService(
    @Value("\${slack.bot.token}") private val slackToken: String
) : NotificationService {

    private val logger = LoggerFactory.getLogger(SlackNotificationService::class.java)
    private val slack = Slack.getInstance()

    override fun sendNotification(to: String, subject: String, body: String) {
        // 'to' 파라미터를 Slack 채널 ID로 사용합니다.
        // 'subject'와 'body'를 조합하여 Slack 메시지를 구성합니다.
        val channelId = to
        val messageText = if (subject.isNotBlank()) {
            "*$subject*\n$body"
        } else {
            body
        }

        if (slackToken.isBlank() || slackToken == "your-slack-bot-token") {
            logger.error("Slack bot token is not configured. Please set SLACK_BOT_TOKEN environment variable or ensure it's correctly read.")
            throw RuntimeException("Slack bot token is not configured.")
        }
        
        if (channelId.isBlank()) {
            logger.error("Slack channel ID (passed as 'to' parameter) is blank. Cannot send Slack notification.")
            // 이 경우, 에러를 던지거나, 기본 채널로 보내는 등의 처리를 고려할 수 있습니다.
            // 여기서는 로깅 후 반환하거나 예외를 던집니다.
            throw IllegalArgumentException("Slack channel ID cannot be blank for SlackNotificationService.")
        }

        try {
            val request = ChatPostMessageRequest.builder()
                .channel(channelId)
                .text(messageText)
                .build()

            val response = slack.methods(slackToken).chatPostMessage(request)

            if (response.isOk) {
                logger.info("Slack notification sent successfully to channel $channelId: ${messageText.take(50)}...")
            } else {
                logger.error("Failed to send Slack notification to channel $channelId. Error: ${response.error}")
                throw RuntimeException("Failed to send Slack notification to channel $channelId. Error: ${response.error}")
            }
        } catch (e: Exception) {
            logger.error("Exception occurred while sending Slack notification to channel $channelId: ${e.message}", e)
            throw RuntimeException("Exception occurred while sending Slack notification to channel $channelId: ${e.message}", e)
        }
    }
    
    // 기존 sendSlackNotification 메서드는 이제 사용되지 않으므로 삭제합니다.
} 