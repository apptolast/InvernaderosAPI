package com.apptolast.invernaderos.features.notification.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * Dedicated executor for FCM send tasks in the notification module.
 *
 * Kept separate from the WebSocket executor so a spike in notification volume
 * cannot starve real-time WebSocket broadcasts — and vice versa.
 *
 * Rejection policy is CallerRunsPolicy (defensive): if the queue is full, the
 * calling thread absorbs the task rather than discarding it, ensuring no push
 * is silently dropped under pressure.
 *
 * Note: @EnableAsync is NOT declared here — it is already enabled globally in
 * WebSocketBroadcastAsyncConfig to avoid duplicate proxy configuration.
 */
@Configuration
class NotificationAsyncConfig {

    @Bean("fcmSendExecutor")
    fun fcmSendExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 8
            queueCapacity = 200
            setThreadNamePrefix("fcm-send-")
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(15)
            initialize()
        }
}
