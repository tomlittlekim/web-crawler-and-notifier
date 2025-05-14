package kr.co.webcrawlerandnotifier.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import kr.co.webcrawlerandnotifier.domain.model.crawler.NotificationType
import org.hibernate.validator.constraints.URL

data class CreateCrawlerRequest(
    @field:NotBlank(message = "URL은 필수입니다.")
    @field:URL(message = "유효한 URL 형식이어야 합니다.")
    val url: String,

    @field:NotBlank(message = "CSS 선택자는 필수입니다.")
    val selector: String,

    @field:NotNull(message = "확인 주기는 필수입니다.")
    @field:Min(value = 60000, message = "확인 주기는 최소 1분(60000ms) 이상이어야 합니다.") // 최소 1분
    val checkInterval: Long, // <--- 필드명을 checkInterval로 변경

    val alertKeyword: String?,

    val alertOnChange: Boolean = false,

    // email은 notificationType이 EMAIL 또는 BOTH일 때만 필수, SLACK일 때는 선택.
    // 복잡한 유효성 검사는 @AssertTrue를 사용한 커스텀 validation 메서드로 구현 가능
    @field:Email(message = "유효한 이메일 형식이어야 합니다.")
    val email: String?, // Slack만 사용할 경우 null 가능하도록 변경

    @field:NotNull(message = "알림 유형은 필수입니다.")
    val notificationType: NotificationType = NotificationType.EMAIL,

    val slackChannelId: String? // notificationType이 SLACK 또는 BOTH일 때 필요
)

data class UpdateCrawlerRequest(
    @field:NotBlank(message = "URL은 필수입니다.")
    @field:URL(message = "유효한 URL 형식이어야 합니다.")
    val url: String,

    @field:NotBlank(message = "CSS 선택자는 필수입니다.")
    val selector: String,

    @field:NotNull(message = "확인 주기는 필수입니다.")
    @field:Min(value = 60000, message = "확인 주기는 최소 1분(60000ms) 이상이어야 합니다.")
    val checkInterval: Long, // <--- 필드명을 checkInterval로 변경

    val alertKeyword: String?,

    val alertOnChange: Boolean = false,

    @field:Email(message = "유효한 이메일 형식이어야 합니다.")
    val email: String?, // Slack만 사용할 경우 null 가능하도록 변경

    @field:NotNull(message = "알림 유형은 필수입니다.")
    val notificationType: NotificationType,

    val slackChannelId: String?
)