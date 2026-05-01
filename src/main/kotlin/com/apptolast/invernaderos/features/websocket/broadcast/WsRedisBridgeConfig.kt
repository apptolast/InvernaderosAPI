package com.apptolast.invernaderos.features.websocket.broadcast

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.Topic
import org.springframework.data.redis.listener.ChannelTopic

/**
 * Wires the Redis pub/sub channel that fans WebSocket broadcasts across
 * pods. The [RedisConnectionFactory] / [RedisTemplate] beans come from the
 * existing [com.apptolast.invernaderos.config.RedisConfig] — we only add a
 * dedicated [RedisMessageListenerContainer] tied to one channel.
 *
 * **Channel naming is environment-prefixed** because Redis pub/sub channels
 * live at the server level, not per-database. Without the prefix a dev pod
 * publishing on the same Redis instance would deliver broadcasts to prod
 * subscribers — a tenant-leakage bug. We pick the prefix from the active
 * Spring profile (`dev`, `prod`, …) so the channel becomes e.g.
 * `inverapi:dev:ws:tenant-status`.
 */
@Configuration
class WsRedisBridgeConfig(
    @param:Value("\${spring.profiles.active:default}") private val activeProfile: String
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean(name = ["wsBroadcastChannel"])
    fun wsBroadcastChannel(): String {
        val channel = "inverapi:${activeProfile}:ws:tenant-status"
        logger.info("WS broadcast Redis channel = {}", channel)
        return channel
    }

    @Bean(name = ["wsBroadcastTopic"])
    fun wsBroadcastTopic(): Topic = ChannelTopic(wsBroadcastChannel())

    @Bean(name = ["wsBroadcastListenerContainer"], destroyMethod = "destroy")
    fun wsBroadcastListenerContainer(
        connectionFactory: RedisConnectionFactory,
        listener: WsRedisListener,
        wsBroadcastTopic: Topic
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(listener, wsBroadcastTopic)
        return container
    }
}
