package kr.co.webcrawlerandnotifier.domain.model.crawler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime

/**
 * 크롤러 도메인 모델 테스트 클래스
 * 
 * Value Object 패턴을 적용한 도메인 모델들의 동작을 검증합니다.
 * Given-When-Then 패턴을 사용하여 테스트의 가독성을 높였습니다.
 * 
 * 테스트 대상:
 * - CrawlerConfiguration: 크롤링 설정 유효성 검사
 * - NotificationConfiguration: 알림 설정 유효성 검사
 * - CrawlerState: 상태 변경 로직
 * - Crawler: 통합 비즈니스 로직
 */
class CrawlerTest {

    @Test
    fun `CrawlerConfiguration 생성 시 유효성 검사`() {
        // given & when & then
        // URL이 비어있는 경우 예외 발생
        assertThrows<IllegalArgumentException> {
            CrawlerConfiguration(
                url = "",
                selector = "div",
                checkIntervalMs = 1000
            )
        }

        // CSS 선택자가 비어있는 경우 예외 발생
        assertThrows<IllegalArgumentException> {
            CrawlerConfiguration(
                url = "https://example.com",
                selector = "",
                checkIntervalMs = 1000
            )
        }

        // 체크 간격이 0 이하인 경우 예외 발생
        assertThrows<IllegalArgumentException> {
            CrawlerConfiguration(
                url = "https://example.com",
                selector = "div",
                checkIntervalMs = 0
            )
        }
    }

    @Test
    fun `NotificationConfiguration 생성 시 유효성 검사`() {
        // given & when & then
        // EMAIL 타입일 때 이메일 주소가 비어있으면 예외 발생
        assertThrows<IllegalArgumentException> {
            NotificationConfiguration(
                email = "",
                notificationType = NotificationType.EMAIL
            )
        }

        // SLACK 타입일 때 채널 ID가 없으면 예외 발생
        assertThrows<IllegalArgumentException> {
            NotificationConfiguration(
                email = "test@example.com",
                notificationType = NotificationType.SLACK,
                slackChannelId = null
            )
        }

        // BOTH 타입일 때 이메일이 비어있으면 예외 발생
        assertThrows<IllegalArgumentException> {
            NotificationConfiguration(
                email = "",
                notificationType = NotificationType.BOTH,
                slackChannelId = "channel-id"
            )
        }
    }

    @Test
    fun `CrawlerState 값 업데이트 테스트`() {
        // given - 초기 상태 생성
        val initialState = CrawlerState()
        val newValue = "새로운 값"

        // when - 새로운 값으로 업데이트
        val updatedState = initialState.withUpdatedValue(newValue)

        // then - 값과 시간이 모두 업데이트되었는지 확인
        assertThat(updatedState.lastCrawledValue).isEqualTo(newValue)
        assertThat(updatedState.lastCheckedAt).isNotNull()
        assertThat(updatedState.lastChangedAt).isNotNull()
    }

    @Test
    fun `CrawlerState 동일한 값 업데이트 시 변경 시간 업데이트 안됨`() {
        // given - 기존 값이 있는 상태 생성
        val existingValue = "기존 값"
        val initialState = CrawlerState(lastCrawledValue = existingValue)

        // when - 동일한 값으로 업데이트
        val updatedState = initialState.withUpdatedValue(existingValue)

        // then - 체크 시간만 업데이트되고 변경 시간은 업데이트되지 않음
        assertThat(updatedState.lastCrawledValue).isEqualTo(existingValue)
        assertThat(updatedState.lastCheckedAt).isNotNull()
        assertThat(updatedState.lastChangedAt).isNull()
    }

    @Test
    fun `Crawler 알림 조건 확인`() {
        // given - 키워드와 변경 감지가 모두 활성화된 크롤러 생성
        val configuration = CrawlerConfiguration(
            url = "https://example.com",
            selector = "div",
            checkIntervalMs = 1000,
            alertKeyword = "특별",
            alertOnChange = true
        )
        val notificationConfig = NotificationConfiguration(
            email = "test@example.com",
            notificationType = NotificationType.EMAIL
        )
        val crawler = Crawler(
            configuration = configuration,
            notificationConfiguration = notificationConfig
        )

        // when & then
        // 키워드가 포함된 내용인 경우 알림 발송
        assertThat(crawler.shouldNotify("특별한 내용")).isTrue()
        
        // 내용이 변경된 경우 알림 발송
        crawler.updateCrawledData("기존 값")
        assertThat(crawler.shouldNotify("새로운 값")).isTrue()
        
        // 조건을 만족하지 않는 경우 알림 발송 안함
        assertThat(crawler.shouldNotify("기존 값")).isFalse()
    }
    
    @Test
    fun `CrawlerConfiguration 헬퍼 메서드 테스트`() {
        // given - 다양한 설정의 크롤러 구성 생성
        val configWithAlert = CrawlerConfiguration(
            url = "https://example.com",
            selector = "div",
            checkIntervalMs = 300000, // 5분
            alertKeyword = "중요",
            alertOnChange = true
        )
        
        val configWithoutAlert = CrawlerConfiguration(
            url = "https://example.com",
            selector = "div",
            checkIntervalMs = 60000 // 1분
        )

        // when & then
        // 체크 간격을 분 단위로 변환
        assertThat(configWithAlert.getCheckIntervalInMinutes()).isEqualTo(5)
        assertThat(configWithoutAlert.getCheckIntervalInMinutes()).isEqualTo(1)
        
        // 알림 조건 설정 여부 확인
        assertThat(configWithAlert.hasAlertCondition()).isTrue()
        assertThat(configWithoutAlert.hasAlertCondition()).isFalse()
    }
    
    @Test
    fun `NotificationConfiguration 헬퍼 메서드 테스트`() {
        // given - 다양한 알림 설정 생성
        val emailConfig = NotificationConfiguration(
            email = "test@example.com",
            notificationType = NotificationType.EMAIL
        )
        
        val slackConfig = NotificationConfiguration(
            email = "test@example.com",
            notificationType = NotificationType.SLACK,
            slackChannelId = "#general"
        )
        
        val bothConfig = NotificationConfiguration(
            email = "test@example.com",
            notificationType = NotificationType.BOTH,
            slackChannelId = "#general"
        )

        // when & then
        // 이메일 활성화 여부 확인
        assertThat(emailConfig.isEmailEnabled()).isTrue()
        assertThat(slackConfig.isEmailEnabled()).isFalse()
        assertThat(bothConfig.isEmailEnabled()).isTrue()
        
        // Slack 활성화 여부 확인
        assertThat(emailConfig.isSlackEnabled()).isFalse()
        assertThat(slackConfig.isSlackEnabled()).isTrue()
        assertThat(bothConfig.isSlackEnabled()).isTrue()
        
        // 이메일 형식 유효성 확인
        assertThat(emailConfig.hasValidEmailFormat()).isTrue()
    }
    
    @Test
    fun `CrawlerState 헬퍼 메서드 테스트`() {
        // given - 다양한 상태의 크롤러 상태 생성
        val activeState = CrawlerState(status = CrawlerStatus.ACTIVE)
        val errorState = CrawlerState(status = CrawlerStatus.ERROR)
        val stateWithValue = CrawlerState(lastCrawledValue = "정상 값")
        val stateWithError = CrawlerState(lastCrawledValue = "Error: 네트워크 오류")

        // when & then
        // 상태 확인
        assertThat(activeState.isActive()).isTrue()
        assertThat(errorState.isError()).isTrue()
        
        // 유효한 값 확인
        assertThat(stateWithValue.hasValidValue()).isTrue()
        assertThat(stateWithError.hasValidValue()).isFalse()
    }
} 