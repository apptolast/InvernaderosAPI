package com.apptolast.invernaderos.features.notification.infrastructure.config

import com.google.firebase.messaging.FirebaseMessagingException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.support.RetryTemplate

@Configuration
@EnableRetry
class NotificationRetryConfig {

    @Bean("fcmRetryTemplate")
    fun fcmRetryTemplate(props: NotificationProperties): RetryTemplate =
        RetryTemplate.builder()
            .maxAttempts(props.fcm.retry.maxAttempts)
            .exponentialBackoff(
                props.fcm.retry.initialDelayMs,
                props.fcm.retry.multiplier,
                props.fcm.retry.maxDelayMs
            )
            .retryOn(FirebaseMessagingException::class.java)
            .build()
}
