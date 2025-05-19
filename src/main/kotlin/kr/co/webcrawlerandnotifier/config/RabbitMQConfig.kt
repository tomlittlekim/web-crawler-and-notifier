package kr.co.webcrawlerandnotifier.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val CRAWLING_EXCHANGE_NAME = "crawling.exchange"
        const val CRAWLING_QUEUE_NAME = "crawling.queue"
        const val CRAWLING_ROUTING_KEY = "crawling.task.route"
    }

    @Bean
    fun crawlingExchange(): TopicExchange {
        return TopicExchange(CRAWLING_EXCHANGE_NAME)
    }

    @Bean
    fun crawlingQueue(): Queue {
        // 내구성 있는 큐 (durable=true). RabbitMQ 서버가 재시작되어도 큐가 유지됩니다.
        return Queue(CRAWLING_QUEUE_NAME, true)
    }

    @Bean
    fun crawlingBinding(crawlingQueue: Queue, crawlingExchange: TopicExchange): Binding {
        return BindingBuilder.bind(crawlingQueue).to(crawlingExchange).with(CRAWLING_ROUTING_KEY)
    }

    /**
     * RabbitMQ 메시지 변환기로 Jackson2JsonMessageConverter를 사용합니다.
     * Kotlin 데이터 클래스 및 Java 8 날짜/시간 타입을 올바르게 직렬화/역직렬화하기 위함입니다.
     */
    @Bean
    fun jsonMessageConverter(): MessageConverter {
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule()) // LocalDateTime 등 Java 8 날짜/시간 타입 지원
        return Jackson2JsonMessageConverter(objectMapper)
    }

    /**
     * RabbitTemplate에 위에서 정의한 MessageConverter를 설정합니다.
     */
    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: MessageConverter): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter
        return rabbitTemplate
    }
} 