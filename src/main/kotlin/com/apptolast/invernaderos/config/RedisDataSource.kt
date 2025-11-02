package com.apptolast.invernaderos.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import io.lettuce.core.api.StatefulConnection
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.time.Duration

@Configuration
@EnableCaching
@EnableRedisRepositories
class RedisConfig(
    @param:Value("\${spring.data.redis.host}")
    private val redisHost: String,

    @param:Value("\${spring.data.redis.port}")
    private val redisPort: Int,

    @param:Value("\${spring.data.redis.password:}")
    private val redisPassword: String?
) {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val redisConfig = RedisStandaloneConfiguration(redisHost, redisPort)
        if (!redisPassword.isNullOrBlank()) {
            redisConfig.setPassword(redisPassword)
        }

        // SOLUCIÓN: Usar raw type (sin generics) como en los ejemplos de Java
        @Suppress("UNCHECKED_CAST")
        val poolConfig = GenericObjectPoolConfig<Any>().apply {
            maxTotal = 100
            maxIdle = 50
            minIdle = 10
            setMaxWait(Duration.ofMillis(3000))
            testOnBorrow = true
            testWhileIdle = true
        }  as GenericObjectPoolConfig<StatefulConnection<*, *>>

        // Configuración del cliente Lettuce pasando el poolConfig directamente
        val clientConfig = LettucePoolingClientConfiguration.builder()
            .commandTimeout(Duration.ofMillis(3000))
            .shutdownTimeout(Duration.ofMillis(100))
            .poolConfig(poolConfig)  // Sin cast - Kotlin lo maneja automáticamente
            .build()

        return LettuceConnectionFactory(redisConfig, clientConfig)
    }

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        // Configurar ObjectMapper para tipos polimórficos
        val redisObjectMapper = objectMapper.copy()
        redisObjectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        )

        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)

        // Configurar serializadores
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = jsonSerializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = jsonSerializer

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val serializer = GenericJackson2JsonRedisSerializer()

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )
            .disableCachingNullValues()

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build()
    }
}