package com.apptolast.invernaderos.features.mqtt

import org.springframework.data.redis.connection.RedisPassword

data class MqttUserDto(
    val username: String,
    val password: String,
    val salt: String? = "apptolast",
    val deviceType: String? = "API",
    val isActive: Boolean? = true,
)
