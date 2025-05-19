package kr.co.webcrawlerandnotifier.application.dto

import java.io.Serializable
import java.util.UUID

/**
 * RabbitMQ로 전송될 크롤링 작업 메시지
 */
data class CrawlingTaskMessage(
    val crawlerId: UUID
) : Serializable // 직렬화를 위해 Serializable 인터페이스 구현 