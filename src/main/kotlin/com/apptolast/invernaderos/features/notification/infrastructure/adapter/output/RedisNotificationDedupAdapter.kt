package com.apptolast.invernaderos.features.notification.infrastructure.adapter.output

import com.apptolast.invernaderos.features.notification.domain.model.NotificationType
import com.apptolast.invernaderos.features.notification.domain.port.output.NotificationDedupPort
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisNotificationDedupAdapter(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val meterRegistry: MeterRegistry
) : NotificationDedupPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun shouldDispatch(
        type: NotificationType,
        alertId: Long,
        userId: Long,
        windowSeconds: Long
    ): Boolean {
        val key = "notif:dedup:${type.name}:$alertId:$userId"
        return try {
            val wasAbsent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(windowSeconds))
            // setIfAbsent returns true when the key was newly set (no duplicate),
            // false when the key already existed (duplicate within window).
            // A null return (connection issue) is treated as fail-open.
            wasAbsent ?: true
        } catch (ex: Exception) {
            logger.warn(
                "Redis dedup failure for type={} alertId={} userId={} — failing open: {}",
                type.name, alertId, userId, ex.message
            )
            meterRegistry.counter("notification.dedup.redis.failures").increment()
            true
        }
    }
}
