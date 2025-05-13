# 나만의 웹 알리미 (Web Crawler & Notifier)

지정한 웹사이트의 특정 정보를 주기적으로 크롤링하고, 조건에 부합할 경우 사용자에게 알림을 보내주는 웹 애플리케이션입니다.

## 주요 기능

* **웹 크롤링 설정:**
    * 크롤링할 웹사이트 URL 지정
    * 추출할 정보의 CSS 선택자 지정
    * 크롤링 주기 설정 (예: 5분, 10분, 1시간 등)
* **알림 조건 설정:**
    * 특정 키워드 포함 시 알림
    * 이전 크롤링 결과와 내용 변경 시 알림
    * 알림 받을 이메일 주소 설정
* **크롤러 관리:**
    * 등록된 크롤러 목록 확인
    * 크롤러 설정 수정 및 삭제
    * 크롤러 상태 (실행 중, 중지됨, 오류 등) 및 최근 크롤링 값 확인
    * 수동으로 즉시 확인 기능
    * 간단한 로그 확인

## 기술 스택

### 백엔드
* Kotlin
* Spring Boot
* (추가 예정: 데이터베이스, 스케줄러, 메일 발송 라이브러리 등)

### 프론트엔드
* HTML
* CSS
* JavaScript
* (간단한 UI 구현을 위해 별도 프레임워크 없이 기본 기능 위주로 작성)

### 데이터베이스
* (추가 예정: H2, PostgreSQL, MySQL 등)

## 프로젝트 구조 (예상)

## 실행 방법

1.  **백엔드 실행:**
    * IDE (IntelliJ 등)에서 `WebCrawlerAndNotifierApplication.kt` 파일을 직접 실행합니다.
    * 또는 터미널에서 `./gradlew bootRun` (또는 `gradlew.bat bootRun`) 명령어를 실행합니다.
    * 기본 포트는 `8757` 입니다. (필요시 `application.properties`에서 변경 가능)

2.  **프론트엔드 접속:**
    * 웹 브라우저에서 `http://localhost:8757/` (또는 `static/index.html`이 제공되는 경로)으로 접속합니다.

## API 엔드포인트 (예상)

* `POST /api/crawlers`: 새 크롤러 등록
* `GET /api/crawlers`: 등록된 크롤러 목록 조회
* `GET /api/crawlers/{id}`: 특정 크롤러 상세 정보 조회
* `PUT /api/crawlers/{id}`: 특정 크롤러 정보 수정
* `DELETE /api/crawlers/{id}`: 특정 크롤러 삭제
* `POST /api/crawlers/{id}/check`: 특정 크롤러 즉시 확인 요청
* `GET /api/crawlers/{id}/logs`: 특정 크롤러 로그 조회 (간단 버전)

## 향후 개선 사항

* 다양한 알림 채널 지원 (Slack, SMS 등)
* 사용자 인증 및 권한 관리
* 고급 크롤링 옵션 (JavaScript 렌더링 지원, 프록시 설정 등)
* 상세 통계 및 대시보드
* Docker를 이용한 배포 간소화

---