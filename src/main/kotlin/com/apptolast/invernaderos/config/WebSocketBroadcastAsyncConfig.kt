package com.apptolast.invernaderos.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

/**
 * Dedicated executor for WebSocket broadcast tasks. Kept isolated from any
 * other `@Async` use case in the codebase so a stuck broadcast can't impact
 * unrelated work — and vice versa.
 *
 * Sizing assumes the natural rate of 1 broadcast/s/tenant from the sensor
 * flush plus rare CRUD/alert events. With small payloads and short
 * `convertAndSendToUser` latencies (≤10 ms locally), 2–4 threads cover
 * dozens of tenants comfortably.
 *
 * `DiscardOldestPolicy` is intentional: a snapshot is idempotent — the
 * newest one always supersedes older ones. If pressure spikes momentarily
 * the queue sheds the oldest pending task and keeps the most recent state
 * flowing to clients. Combined with the queue capacity of 200 this gives a
 * hard upper bound on memory while preserving correctness under load.
 */
@Configuration
@EnableAsync
class WebSocketBroadcastAsyncConfig {

    @Bean(name = ["wsBroadcastExecutor"], destroyMethod = "shutdown")
    fun wsBroadcastExecutor(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 200
            setThreadNamePrefix("ws-broadcast-")
            setRejectedExecutionHandler(ThreadPoolExecutor.DiscardOldestPolicy())
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            initialize()
        }
    }
}
