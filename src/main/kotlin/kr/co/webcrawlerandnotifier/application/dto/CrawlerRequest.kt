package kr.co.webcrawlerandnotifier.application.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.URL

data class CreateCrawlerRequest(
    @field:NotBlank(message = "URL은 필수입니다.")
    @field:URL(message = "유효한 URL 형식이어야 합니다.")
    val url: String,

    @field:NotBlank(message = "CSS 선택자는 필수입니다.")
    val selector: String,

    @field:NotNull(message = "확인 주기는 필수입니다.")
    @field:Min(value = 60000, message = "확인 주기는 최소 1분(60000ms) 이상이어야 합니다.") // 최소 1분
    val checkIntervalMs: Long,

    val alertKeyword: String?,

    val alertOnChange: Boolean = false,

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이어야 합니다.")
    val email: String
)

data class UpdateCrawlerRequest(
    @field:NotBlank(message = "URL은 필수입니다.")
    @field:URL(message = "유효한 URL 형식이어야 합니다.")
    val url: String,

    @field:NotBlank(message = "CSS 선택자는 필수입니다.")
    val selector: String,

    @field:NotNull(message = "확인 주기는 필수입니다.")
    @field:Min(value = 60000, message = "확인 주기는 최소 1분(60000ms) 이상이어야 합니다.")
    val checkIntervalMs: Long,

    val alertKeyword: String?,

    val alertOnChange: Boolean = false,

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이어야 합니다.")
    val email: String
)