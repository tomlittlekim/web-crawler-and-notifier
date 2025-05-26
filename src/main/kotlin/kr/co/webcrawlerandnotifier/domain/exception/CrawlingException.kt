package kr.co.webcrawlerandnotifier.domain.exception

/**
 * 크롤링 관련 예외의 기본 클래스
 * 
 * Sealed Class를 사용하여 타입 안전한 예외 처리를 제공합니다.
 * 모든 크롤링 관련 예외는 이 클래스를 상속받아야 하며,
 * when 표현식에서 모든 경우를 처리했는지 컴파일 타임에 검증할 수 있습니다.
 * 
 * @param message 예외 메시지
 * @param cause 원인이 되는 예외 (선택적)
 */
sealed class CrawlingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 크롤링 타임아웃 예외
 * 
 * 지정된 시간 내에 크롤링이 완료되지 않았을 때 발생합니다.
 * 
 * @param url 크롤링 대상 URL
 * @param timeout 타임아웃 시간 (밀리초)
 */
class CrawlingTimeoutException(url: String, timeout: Long) : 
    CrawlingException("크롤링 타임아웃: URL=$url, timeout=${timeout}ms")

/**
 * 크롤링 네트워크 예외
 * 
 * 네트워크 연결 문제로 크롤링이 실패했을 때 발생합니다.
 * 예: 연결 거부, DNS 해석 실패, 호스트 접근 불가 등
 * 
 * @param url 크롤링 대상 URL
 * @param cause 원인이 되는 네트워크 예외
 */
class CrawlingNetworkException(url: String, cause: Throwable) : 
    CrawlingException("네트워크 오류: URL=$url", cause)

/**
 * 크롤링 파싱 예외
 * 
 * HTML 파싱 과정에서 오류가 발생했을 때 발생합니다.
 * 예: 잘못된 HTML 구조, CSS 선택자로 요소를 찾을 수 없음 등
 * 
 * @param url 크롤링 대상 URL
 * @param selector 사용된 CSS 선택자
 * @param cause 원인이 되는 파싱 예외 (선택적)
 */
class CrawlingParseException(url: String, selector: String, cause: Throwable? = null) : 
    CrawlingException("파싱 오류: URL=$url, selector=$selector", cause)

/**
 * 유효하지 않은 CSS 선택자 예외
 * 
 * 제공된 CSS 선택자가 유효하지 않을 때 발생합니다.
 * 
 * @param selector 유효하지 않은 CSS 선택자
 */
class InvalidSelectorException(selector: String) : 
    CrawlingException("유효하지 않은 CSS 선택자: $selector")

/**
 * 알림 발송 예외
 * 
 * 이메일이나 Slack 알림 발송 과정에서 오류가 발생했을 때 발생합니다.
 * 
 * @param message 알림 발송 실패 상세 메시지
 * @param cause 원인이 되는 예외 (선택적)
 */
class NotificationException(message: String, cause: Throwable? = null) : 
    CrawlingException("알림 발송 실패: $message", cause) 