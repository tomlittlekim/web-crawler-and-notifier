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
    * 알림 받을 이메일 주소 설정
* **크롤러 관리:**
    * 등록된 크롤러 목록 확인
    * 크롤러 설정 수정 및 삭제
    * 크롤러 상태 (ACTIVE, INACTIVE, ERROR, PENDING) 및 최근 크롤링 값, 최근 확인 시간, 최근 변경 시간 확인
    * 수동으로 즉시 확인 기능
    * 크롤러별 최근 5개 로그 확인 기능 (성공/실패 여부, 오류 메시지, 알림 발송 여부 포함)

## 기술 스택

### 백엔드
* Kotlin (`1.9.25`)
* Spring Boot (`3.4.5`)
    * Spring Web
    * Spring Data JPA
    * Spring Mail (Gmail SMTP 사용)
    * Spring Validation
* Jsoup (`1.17.2`): HTML 파싱 라이브러리

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
        * `dto`: `CrawlerRequest.kt`, `CrawlerResponse.kt` 등 API 요청/응답 객체
        * `service`: `CrawlerAppService.kt` 등 핵심 비즈니스 로직 처리
    * `config`: 애플리케이션 설정 (`SchedulingConfig.kt` 등)
    * `domain`: 도메인 모델, 리포지토리 인터페이스, 도메인 서비스, 예외
        * `model`: `Crawler.kt`, `CrawlLog.kt`, `CrawlerStatus.kt` 등 핵심 엔티티
        * `repository`: `CrawlerRepository.kt`, `CrawlLogRepository.kt` 등 JPA 리포지토리 인터페이스
        * `exception`: `CrawlerNotFoundException.kt` 등 사용자 정의 예외
    * `infrastructure`: 외부 시스템 연동 구현 (크롤링, 알림, 스케줄링)
        * `crawling`: `WebCrawlerService.kt` (Jsoup 기반 실제 크롤링 로직)
        * `notification`: `NotificationService.kt` (Spring Mail을 이용한 이메일 발송 로직)
        * `scheduler`: `CrawlerScheduler.kt` (등록된 크롤러 주기적 실행)
    * `presentation`: 외부 요청 처리 (API 컨트롤러, 예외 핸들러)
        * `controller`: `CrawlerController.kt`, `GlobalExceptionHandler.kt`
* `resources`: 설정 파일 및 정적 리소스
    * `application.properties`: 애플리케이션 주요 설정 (DB, Mail 등)
    * `static`: HTML, CSS, JavaScript 파일

## 실행 방법

1.  **백엔드 실행:**
    * IDE (IntelliJ 등)에서 `WebCrawlerAndNotifierApplication.kt` 파일을 직접 실행합니다.
    * 또는 터미널에서 `./gradlew bootRun` (또는 `gradlew.bat bootRun`) 명령어를 실행합니다.
    * 기본 애플리케이션 포트는 `8080` 입니다 (별도 설정이 없는 경우 Spring Boot 기본값).
    * H2 데이터베이스 콘솔은 `http://localhost:8080/h2-console` 경로로 접속 가능합니다 (application.properties 설정 기준, JDBC URL: `jdbc:h2:file:./data/crawlerdb`, 사용자 이름: `sa`, 비밀번호: 없음).

2.  **프론트엔드 접속:**
    * 웹 브라우저에서 `http://localhost:8080/` (또는 `static/index.html`이 제공되는 경로)으로 접속합니다.

## API 엔드포인트

* `POST /api/crawlers`: 새 크롤러 등록
* `GET /api/crawlers`: 등록된 크롤러 목록 조회
* `GET /api/crawlers/{id}`: 특정 크롤러 상세 정보 조회
* `PUT /api/crawlers/{id}`: 특정 크롤러 정보 수정
* `DELETE /api/crawlers/{id}`: 특정 크롤러 삭제
* `POST /api/crawlers/{id}/check`: 특정 크롤러 즉시 확인 요청
* `GET /api/crawlers/{id}/logs`: 특정 크롤러 로그 조회 (최근 5개)
* `PUT /api/crawlers/{id}/activate`: 특정 크롤러 활성화 (현재 미구현, `NotImplementedError` 발생)
* `PUT /api/crawlers/{id}/deactivate`: 특정 크롤러 비활성화 (현재 미구현, `NotImplementedError` 발생)

## 향후 개선 사항

* 크롤러 활성/비활성 기능 구현 (`activateCrawler`, `deactivateCrawler` API)
* 다양한 알림 채널 지원 (Slack, SMS 등)
* 사용자 인증 및 권한 관리
* 고급 크롤링 옵션 (JavaScript 렌더링 지원, 프록시 설정 등)
* 상세 통계 및 대시보드
* Docker를 이용한 배포 간소화
* 스케줄링 로직 고도화 (예: 분산 환경 고려, DB 부하 분산)