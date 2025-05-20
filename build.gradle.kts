import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25" // 기존 버전 유지 또는 최신 안정 버전
    kotlin("plugin.spring") version "1.9.25" // 기존 버전 유지 또는 최신 안정 버전
    kotlin("plugin.jpa") version "1.9.25" // JPA 플러그인 추가
    kotlin("kapt") version "1.9.25" // kotlin-kapt 플러그인 추가
    id("org.springframework.boot") version "3.4.5" // 기존 버전 유지 또는 최신 안정 버전
    id("io.spring.dependency-management") version "1.1.7" // 기존 버전 유지 또는 최신 안정 버전
    id("com.ewerk.gradle.plugins.querydsl") version "1.0.10" // QueryDSL 플러그인 추가
}

group = "kr.co"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// QueryDSL 설정
querydsl {
    jpa = true
    querydslSourcesDir = "build/generated/source/kapt/main" // 문자열 경로로 수정
    library = "com.querydsl:querydsl-jpa:5.1.0:jakarta"
}

repositories {
    mavenCentral()
}

val queryDslVersion = "5.1.0"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail") // 메일 발송
    implementation("org.springframework.boot:spring-boot-starter-validation") // 유효성 검사
    implementation("org.springframework.boot:spring-boot-starter-amqp") // RabbitMQ

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Kotlin용 Jackson 모듈

    // Database (H2는 인메모리 DB로 개발/테스트에 용이, 추후 변경 가능)
    runtimeOnly("com.h2database:h2")

    // Web Crawling
    implementation("org.jsoup:jsoup:1.17.2") // Jsoup HTML 파서

    // Slack
    implementation("com.slack.api:slack-api-client:1.45.3")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:${queryDslVersion}:jakarta")
    kapt("com.querydsl:querydsl-apt:${queryDslVersion}:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // ShedLock for distributed scheduling
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.14.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.14.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// compileKotlin 작업이 compileQuerydsl 작업에 의존하도록 설정
tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
    dependsOn(tasks.named("compileQuerydsl"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// JPA 플러그인 사용을 위한 설정 (no-arg constructor)
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

noArg {
    annotation("jakarta.persistence.Entity")
}