package kr.co.webcrawlerandnotifier.domain.statistics

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "statistics")
class Statistics(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val eventTimestamp: LocalDateTime,

    @Column(nullable = false)
    val eventType: String,

    val targetUrl: String? = null,

    val durationMs: Long? = null,

    @Column(nullable = false)
    val isSuccess: Boolean,

    @Lob // 긴 텍스트를 저장하기 위해 @Lob 어노테이션 사용 고려
    val details: String? = null
) 