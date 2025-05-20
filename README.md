# 나만의 웹 알리미 (Web Crawler & Notifier)

지정한 웹사이트의 특정 정보를 주기적으로 크롤링하고, 조건에 부합할 경우 사용자에게 알림을 보내주는 웹 애플리케이션입니다.

## 주요 기능

* **웹 크롤링 설정:**
    * 크롤링할 웹사이트 URL 지정
    * 추출할 정보의 CSS 선택자 지정
    * 크롤링 주기 설정 (예: 5분, 10분, 30분, 1시간, 매일 등)
* **알림 조건 설정:**
    * 특정 키워드 포함 시 알림
    * 이전 크롤링 결과와 내용 변경 시 알림
    * 알림 받을 이메일 주소 또는 Slack 채널 ID 설정
    * 알림 유형 선택 (이메일, Slack, 또는 둘 다)
* **크롤러 관리:**
    * 등록된 크롤러 목록 확인
    * 크롤러 설정 수정 및 삭제
    * 크롤러 상태 (ACTIVE, INACTIVE, ERROR, PENDING) 및 최근 크롤링 값, 최근 확인 시간, 최근 변경 시간 확인
    * 수동으로 즉시 확인 기능
    * 크롤러별 최근 5개 로그 확인 기능 (성공/실패 여부, 오류 메시지, 알림 발송 여부 포함)
    * 크롤러 활성/비활성 기능
* **통계 대시보드:**
    * 전체 크롤링 시도 및 성공/실패 현황 시각화
    * 이벤트 유형(성공, 실패, 변경 감지 등)별 요약 통계 (차트 및 테이블)
    * 최근 발생한 오류 로그 목록 (최대 10개)
    * URL별 크롤링 성공/실패 통계
    * 콘텐츠 길이에 따른 UI 깨짐 방지 및 반응형 레이아웃 개선

## 기술 스택

### 백엔드
* Kotlin (`1.9.25`)
* Spring Boot (`3.4.5`)
    * Spring Web
    * Spring Data JPA
    * Spring Mail (Gmail SMTP 사용)
    * Spring Validation
    * Spring AMQP (RabbitMQ 연동)
* Jsoup (`1.17.2`): HTML 파싱 라이브러리
* Slack API Client (`com.slack.api:slack-api-client:1.45.3`): Slack 알림 연동
* RabbitMQ: 메시지 큐
* ShedLock: 분산 스케줄링 잠금

### 프론트엔드
* HTML
* CSS
* JavaScript (Vanilla JS)

### 데이터베이스
* H2 (File-based)
    * 개발 및 테스트 용도로 사용. (`spring.jpa.hibernate.ddl-auto=update` 로 설정되어 애플리케이션 실행 시 스키마 자동 업데이트)

## 프로젝트 구조 (주요 패키지)

* `kr.co.webcrawlerandnotifier`: 루트 패키지
    * `application`: 애플리케이션 서비스 및 DTO (Data Transfer Objects)
        * `dto`: `CrawlerRequest.kt`, `CrawlerResponse.kt`, `CrawlingTaskMessage.kt` 등 API 요청/응답 객체 및 메시지 DTO
        * `service`: `CrawlerAppService.kt` 등 핵심 비즈니스 로직 처리
    * `config`: 애플리케이션 설정 (`SchedulingConfig.kt`, `RabbitMQConfig.kt` 등)
    * `domain`: 도메인 모델, 리포지토리 인터페이스, 도메인 서비스, 예외
        * `model`: `Crawler.kt`, `CrawlLog.kt`, `CrawlerStatus.kt`, `NotificationType.kt` 등 핵심 엔티티
        * `repository`: `CrawlerRepository.kt`, `CrawlLogRepository.kt` 등 JPA 리포지토리 인터페이스
        * `exception`: `CrawlerNotFoundException.kt` 등 사용자 정의 예외
    * `infrastructure`: 외부 시스템 연동 구현 (크롤링, 알림, 스케줄링, 메시징)
        * `crawling`: `WebCrawlerService.kt` (Jsoup 기반 실제 크롤링 로직)
        * `notification`: `EmailNotificationService.kt`, `SlackNotificationService.kt` (Spring Mail 및 Slack API를 이용한 알림 발송 로직)
        * `scheduler`: `CrawlerScheduler.kt` (주기적인 크롤링 작업 메시지 발행)
        * `messaging`: `CrawlingMessageListener.kt` (RabbitMQ 메시지 수신 및 크롤링 작업 처리)
    * `presentation`: 외부 요청 처리 (API 컨트롤러, 예외 핸들러)
        * `controller`: `CrawlerController.kt`, `GlobalExceptionHandler.kt`
* `resources`: 설정 파일 및 정적 리소스
    * `application.properties`: 애플리케이션 주요 설정 (DB, Mail, Slack 등)
    * `static`: HTML, CSS, JavaScript 파일

## 실행 방법

1.  **선행 조건:**
    * **RabbitMQ 서버 실행:** 크롤링 작업을 비동기적으로 처리하기 위해 RabbitMQ 서버가 실행 중이어야 합니다. (예: `localhost:5672`에서 실행)
        * Docker 사용 시: `docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management`

2.  **백엔드 실행:**
    * IDE (IntelliJ 등)에서 `WebCrawlerAndNotifierApplication.kt` 파일을 직접 실행합니다.
    * 또는 터미널에서 `./gradlew bootRun` (또는 `gradlew.bat bootRun`) 명령어를 실행합니다.
    * 기본 애플리케이션 포트는 `8080` 입니다 (별도 설정이 없는 경우 Spring Boot 기본값).
    * H2 데이터베이스 콘솔은 `http://localhost:8080/h2-console` 경로로 접속 가능합니다 (application.properties 설정 기준, JDBC URL: `jdbc:h2:file:./data/crawlerdb`, 사용자 이름: `sa`, 비밀번호: 없음).
    * **Slack 알림 설정:** Slack 알림을 사용하려면 `application.properties` 파일 또는 환경 변수를 통해 `slack.bot.token`을 설정해야 합니다.
    * **이메일 알림 설정:** 이메일 알림을 사용하려면 `application.properties` 파일 또는 환경 변수를 통해 `spring.mail.username`과 `spring.mail.password`를 설정해야 합니다.
    * **RabbitMQ 연결 정보:** 필요한 경우 `application.properties` 파일에서 `spring.rabbitmq.*` 관련 설정을 수정합니다. (기본값: `localhost:5672`, `guest`/`guest`)

3.  **프론트엔드 접속:**
    * 웹 브라우저에서 `http://localhost:8080/` (또는 `static/index.html`이 제공되는 경로)으로 접속합니다.

## API 엔드포인트

* `POST /api/crawlers`: 새 크롤러 등록
* `GET /api/crawlers`: 등록된 크롤러 목록 조회
* `GET /api/crawlers/{id}`: 특정 크롤러 상세 정보 조회
* `PUT /api/crawlers/{id}`: 특정 크롤러 정보 수정
* `DELETE /api/crawlers/{id}`: 특정 크롤러 삭제
* `POST /api/crawlers/{id}/check`: 특정 크롤러 즉시 확인 요청
* `GET /api/crawlers/{id}/logs`: 특정 크롤러 로그 조회 (최근 5개)
* `PUT /api/crawlers/{id}/activate`: 특정 크롤러 활성화
* `PUT /api/crawlers/{id}/deactivate`: 특정 크롤러 비활성화

## 향후 개선 사항

* SMS 등 추가 알림 채널 지원
* 사용자 인증 및 권한 관리
* 고급 크롤링 옵션 (JavaScript 렌더링 지원, 프록시 설정 등)
* Docker를 이용한 배포 간소화
* **스케줄링 및 메시징 시스템 고도화:**
    * RabbitMQ Dead Letter Queue(DLQ) 설정 및 실패한 메시지 관리 방안 구체화
    * 메시지 처리량 및 컨슈머 모니터링 시스템 구축
    * DB 부하 분산을 위한 추가적인 전략 (예: 크롤링 작업의 우선순위 부여, 실행 간격 동적 조절 등)
* 테스트 코드 커버리지 확대