package kr.co.webcrawlerandnotifier.infrastructure.persistence.statistics

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.co.webcrawlerandnotifier.domain.statistics.EventTypeProjection
import kr.co.webcrawlerandnotifier.domain.statistics.QStatistics.statistics // 생성된 QStatistics 사용
import kr.co.webcrawlerandnotifier.domain.statistics.Statistics
import kr.co.webcrawlerandnotifier.domain.statistics.StatisticsRepositoryCustom
import kr.co.webcrawlerandnotifier.domain.statistics.UrlCrawlStatProjection
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository // Spring이 빈으로 인식하도록
class StatisticsRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : StatisticsRepositoryCustom {

    override fun getEventTypeSummariesWithQueryDsl(): List<EventTypeProjection> {
        val qStat = statistics
        return queryFactory
            .select(
                Projections.constructor(
                    EventTypeProjection::class.java,
                    qStat.eventType,
                    qStat.id.count(),
                    Expressions.cases()
                        .`when`(qStat.isSuccess.isTrue).then(1L)
                        .otherwise(0L)
                        .sum()
                )
            )
            .from(qStat)
            .groupBy(qStat.eventType)
            .fetch()
    }

    override fun findRecentErrorsWithQueryDsl(pageable: Pageable): List<Statistics> {
        val qStat = statistics
        return queryFactory
            .selectFrom(qStat)
            .where(qStat.isSuccess.isFalse)
            .orderBy(qStat.eventTimestamp.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
    }

    override fun getUrlCrawlStatsWithQueryDsl(): List<UrlCrawlStatProjection> {
        val qStat = statistics

        return queryFactory
            .select(
                Projections.constructor(
                    UrlCrawlStatProjection::class.java,
                    qStat.targetUrl,
                    qStat.id.count(), // totalAttempts
                    Expressions.cases()
                        .`when`(qStat.isSuccess.isTrue).then(1L)
                        .otherwise(0L)
                        .sum(), // successCount
                    Expressions.cases()
                        .`when`(qStat.isSuccess.isFalse).then(1L)
                        .otherwise(0L)
                        .sum(), // failureCount
                    Expressions.cases()
                        .`when`(qStat.isSuccess.isTrue).then(qStat.durationMs)
                        .otherwise(null as Long?)
                        .avg(), // averageDurationMsSuccess
                    Expressions.cases()
                        .`when`(qStat.isSuccess.isFalse).then(qStat.eventTimestamp)
                        .otherwise(null as LocalDateTime?)
                        .max(), // lastFailureTimestamp
                    qStat.eventTimestamp.max() // lastCheckedAt
                )
            )
            .from(qStat)
            .where(qStat.eventType.eq("CRAWL_ATTEMPT").and(qStat.targetUrl.isNotNull))
            .groupBy(qStat.targetUrl)
            .fetch()
    }
} 